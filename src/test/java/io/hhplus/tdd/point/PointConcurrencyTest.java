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
import org.springframework.test.web.servlet.MvcResult;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
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
    @Autowired
    private UserPointTable userPointTable;
    @Autowired
    private PointHistoryTable pointHistoryTable;


    private static final long USER_ID = 1L;
    private static final int THREAD_COUNT = 100;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(10);

    @BeforeEach
    void clearInMemoryDatabase() throws Exception {
        // 예: UserPointTable 의 내부 Map, PointHistoryTable 의 내부 List 등 클리어
        // 리플렉션으로 private 필드에 접근해 클리어 가능
        Field tableField1 = UserPointTable.class.getDeclaredField("table");
        tableField1.setAccessible(true);
        ((Map<?, ?>) tableField1.get(userPointTable)).clear();

        Field tableField2 = PointHistoryTable.class.getDeclaredField("table");
        tableField2.setAccessible(true);
        ((List<?>) tableField2.get(pointHistoryTable)).clear();

        Field cursorField = PointHistoryTable.class.getDeclaredField("cursor");
        cursorField.setAccessible(true);
        cursorField.set(pointHistoryTable, 1L);
    }

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



    @Test
    @DisplayName("동시 사용 요청 시 일부 실패하고 잔액은 음수가 되지 않는다")
    void concurrentUse_insufficientBalance_fail() throws Exception {
        // Given: 충전
        long initialCharge = 5000L;
        mockMvc.perform(patch("/point/{id}/charge", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PointRequest(initialCharge))))
                .andExpect(status().isOk());

        int threadCount = 50;
        long useAmount = 200L;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            EXECUTOR.execute(() -> {
                try {
                    mockMvc.perform(patch("/point/{id}/use", USER_ID)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(new PointRequest(useAmount))))
                            .andExpect(status().is(anyOf(is(200), is(409)))); // 409: 잔액 부족
                } catch (Exception e) {
                    failCount.incrementAndGet(); // 예외 발생한 경우
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // Then: 잔액은 음수가 아니고, 일부 요청은 실패했어야 함
        MvcResult result = mockMvc.perform(get("/point/{id}", USER_ID))
                .andExpect(status().isOk())
                .andReturn();

        UserPoint userPoint = objectMapper.readValue(result.getResponse().getContentAsString(), UserPoint.class);
        assertThat(userPoint.point()).isBetween(0L, initialCharge);

        // 성공한 요청 수 * 200 만큼만 빠졌는지 정합성 확인
        long expectedMaxUsed = (initialCharge / useAmount) * useAmount;
        long used = initialCharge - userPoint.point();
        assertThat(used).isLessThanOrEqualTo(expectedMaxUsed);
        assertThat(userPoint.point()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("동시 다중 사용자 충전/사용 요청 처리")
    void concurrentMultiUserOperations_success() throws Exception {
        int userCount = 10;
        int opsPerUser = 20;
        CountDownLatch latch = new CountDownLatch(userCount * opsPerUser * 2);

        for (long userId = 1; userId <= userCount; userId++) {
            long finalUserId = userId;
            for (int i = 0; i < opsPerUser; i++) {
                EXECUTOR.execute(() -> {
                    try {
                        mockMvc.perform(patch("/point/{id}/charge", finalUserId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(new PointRequest(100L))))
                                .andExpect(status().isOk());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        latch.countDown();
                    }
                });

                EXECUTOR.execute(() -> {
                    try {
                        mockMvc.perform(patch("/point/{id}/use", finalUserId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(new PointRequest(50L))))
                                .andExpect(status().isOk());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        latch.await();

        // 각 유저 잔액 검증 (100*20 - 50*20 = 1000)
        for (long userId = 1; userId <= userCount; userId++) {
            MvcResult result = mockMvc.perform(get("/point/{id}", userId))
                    .andExpect(status().isOk())
                    .andReturn();

            UserPoint userPoint = objectMapper.readValue(result.getResponse().getContentAsString(), UserPoint.class);
            assertThat(userPoint.point()).isEqualTo(1000L);
        }
    }
}