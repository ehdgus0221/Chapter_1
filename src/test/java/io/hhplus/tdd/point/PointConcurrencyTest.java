package io.hhplus.tdd.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.hhplus.tdd.point.dto.PointRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Point 동시성 통합 테스트")
public class PointConcurrencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final long USER_ID = 1L;
    private static final int THREAD_COUNT = 100;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(10);

    @Test
    @DisplayName("동일 사용자에 대해 충전 100번 동시 요청 시 누락 없이 반영된다")
    void concurrentCharge_success() throws Exception {
        long chargeAmount = 100L;
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            EXECUTOR.execute(() -> {
                try {
                    mockMvc.perform(patch("/point/{id}/charge", USER_ID)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(new PointRequest(chargeAmount))))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 쓰레드가 끝날 때까지 대기

        // 최종 잔고 검증
        MvcResult result = mockMvc.perform(get("/point/{id}", USER_ID))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        UserPoint userPoint = objectMapper.readValue(content, UserPoint.class);
        assertThat(userPoint.point()).isEqualTo(chargeAmount * THREAD_COUNT);
    }
}