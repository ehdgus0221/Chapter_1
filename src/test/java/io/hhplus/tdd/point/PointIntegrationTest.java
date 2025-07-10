package io.hhplus.tdd.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.dto.PointRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Point 통합 테스트")
public class PointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final long USER_ID = 1L;

    @Autowired
    private UserPointTable userPointTable;
    @Autowired
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void clearInMemoryDatabase() throws Exception {
        // 테스트 독립성을 위한 메모리 초기화
        // 리플렉션으로 private 필드에 접근해 클리어 가능
        Field tableField1 = UserPointTable.class.getDeclaredField("table");
        tableField1.setAccessible(true);
        ((Map<?, ?>) tableField1.get(userPointTable)).clear();

        Field tableField2 = PointHistoryTable.class.getDeclaredField("table");
        tableField2.setAccessible(true);
        ((List<?>) tableField2.get(pointHistoryTable)).clear();

        // cursor도 초기화
        Field cursorField = PointHistoryTable.class.getDeclaredField("cursor");
        cursorField.setAccessible(true);
        cursorField.set(pointHistoryTable, 1L);
    }

    @Test
    @DisplayName("포인트 충전 성공")
    void charge_success() throws Exception {
        long chargeAmount = 3000L;

        mockMvc.perform(patch("/point/{id}/charge", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PointRequest(chargeAmount))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.point").value(chargeAmount));

        // 히스토리 확인
        mockMvc.perform(get("/point/{id}/histories", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[0].amount").value(chargeAmount));
    }

    @Test
    @DisplayName("포인트 사용 성공")
    void use_success() throws Exception {
        long chargeAmount = 3000L;
        long useAmount = 1000L;

        mockMvc.perform(patch("/point/{id}/charge", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PointRequest(chargeAmount))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/point/{id}/use", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PointRequest(useAmount))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(chargeAmount - useAmount));

        mockMvc.perform(get("/point/{id}/histories", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].type").value("USE"))
                .andExpect(jsonPath("$[1].amount").value(useAmount));
    }

    @Test
    @DisplayName("포인트 조회")
    void getPoint_success() throws Exception {
        long chargeAmount = 1000L;
        mockMvc.perform(patch("/point/{id}/charge", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PointRequest(chargeAmount))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/point/{id}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.point").value(chargeAmount));
    }

    @Test
    @DisplayName("포인트 충전 실패 - 음수 입력")
    void charge_negativeAmount_fail() throws Exception {
        mockMvc.perform(patch("/point/{id}/charge", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PointRequest(-100L))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("포인트 충전 실패 - 최대 초과")
    void charge_exceedMax_fail() throws Exception {
        mockMvc.perform(patch("/point/{id}/charge", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PointRequest(1_000_001L))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("포인트 사용 실패 - 잔액 부족")
    void use_insufficientBalance_fail() throws Exception {
        mockMvc.perform(patch("/point/{id}/use", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PointRequest(5000L))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("포인트 사용 실패 - 음수 입력")
    void use_negativeAmount_fail() throws Exception {
        mockMvc.perform(patch("/point/{id}/use", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PointRequest(-500L))))
                .andExpect(status().isBadRequest());
    }
}
