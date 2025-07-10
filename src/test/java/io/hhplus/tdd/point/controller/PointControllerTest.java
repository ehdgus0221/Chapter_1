package io.hhplus.tdd.point.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.exception.MaxChargeAmountException;
import io.hhplus.tdd.exception.MinChargeAmountException;
import io.hhplus.tdd.point.*;
import io.hhplus.tdd.point.dto.PointRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(PointController.class)
@DisplayName("PointController 단위 테스트")
public class PointControllerTest {

    private static final long USER_ID = 1L;
    private static final long NOW = System.currentTimeMillis();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PointService pointService;

    @Test
    @DisplayName("포인트 조회 - 성공")
    void getPoint_success() throws Exception {
        UserPoint userPoint = new UserPoint(USER_ID, 5000L, NOW);
        given(pointService.getPoint(USER_ID)).willReturn(userPoint);

        mockMvc.perform(get("/point/{id}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.point").value(5000));
    }

    @Test
    @DisplayName("포인트 히스토리 조회 - 성공")
    void getPointHistories_success() throws Exception {
        List<PointHistory> histories = List.of(
                new PointHistory(1L, USER_ID, 1000L, TransactionType.CHARGE, NOW),
                new PointHistory(2L, USER_ID, 500L, TransactionType.USE, NOW)
        );
        given(pointService.getHistories(USER_ID)).willReturn(histories);

        mockMvc.perform(get("/point/{id}/histories", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[1].type").value("USE"));
    }

    @Test
    @DisplayName("포인트 충전 - 성공")
    void chargePoint_success() throws Exception {
        long chargeAmount = 1000L;
        UserPoint chargedPoint = new UserPoint(USER_ID, 1000L, NOW);

        given(pointService.charge(eq(USER_ID), eq(chargeAmount))).willReturn(chargedPoint);

        mockMvc.perform(patch("/point/{id}/charge", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PointRequest(chargeAmount))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(1000));
    }

    @Test
    @DisplayName("포인트 사용 - 성공")
    void usePoint_success() throws Exception {
        long useAmount = 500L;
        UserPoint updated = new UserPoint(USER_ID, 1500L, NOW);

        given(pointService.use(eq(USER_ID), eq(useAmount))).willReturn(updated);

        mockMvc.perform(patch("/point/{id}/use", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PointRequest(useAmount))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(1500));
    }

    @Test
    @DisplayName("포인트 충전 - 음수 입력 시 400 반환")
    void chargePoint_negativeAmount_returns400() throws Exception {
        long invalidAmount = -1000L;

        given(pointService.charge(USER_ID, invalidAmount))
                .willThrow(new IllegalArgumentException("음수는 충전할 수 없습니다"));

        mockMvc.perform(patch("/point/{id}/charge", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PointRequest(invalidAmount))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ILLEGAL_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("음수는 충전할 수 없습니다"));
    }

    @Test
    @DisplayName("포인트 충전 - 최소 금액 미만 시 400 반환")
    void chargePoint_belowMinAmount_returns400() throws Exception {
        long invalidAmount = 50L;

        given(pointService.charge(USER_ID, invalidAmount))
                .willThrow(new MinChargeAmountException());

        mockMvc.perform(patch("/point/{id}/charge", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PointRequest(invalidAmount))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MIN_CHARGE_ERROR"))
                .andExpect(jsonPath("$.message").value("최소 충전 금액은 100원입니다"));
    }

    @Test
    @DisplayName("포인트 충전 - 최대 금액 초과 시 400 반환")
    void chargePoint_overMaxAmount_returns400() throws Exception {
        long invalidAmount = 1_000_001L;

        given(pointService.charge(USER_ID, invalidAmount))
                .willThrow(new MaxChargeAmountException());

        mockMvc.perform(patch("/point/{id}/charge", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PointRequest(invalidAmount))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MAX_CHARGE_ERROR"))
                .andExpect(jsonPath("$.message").value("최대 충전 금액은 100만원입니다"));
    }

    @Test
    @DisplayName("포인트 사용 - 잔액 부족 시 409 반환")
    void usePoint_insufficientBalance_returns409() throws Exception {
        long useAmount = 10000L;

        given(pointService.use(USER_ID, useAmount))
                .willThrow(new IllegalStateException("잔액이 부족합니다."));

        mockMvc.perform(patch("/point/{id}/use", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PointRequest(useAmount))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"))
                .andExpect(jsonPath("$.message").value("잔액이 부족합니다."));
    }

    @Test
    @DisplayName("포인트 충전 - amount에 문자열 입력 시 400 반환")
    void chargePoint_stringAmount_returns400() throws Exception {
        String payload = "{\"amount\": \"string\"}";

        mockMvc.perform(patch("/point/{id}/charge", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("포인트 충전 - Content-Type이 없으면 415 반환")
    void chargePoint_noContentType_returns415() throws Exception {
        mockMvc.perform(patch("/point/{id}/charge", USER_ID)
                        .content("{\"amount\": 1000}"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
