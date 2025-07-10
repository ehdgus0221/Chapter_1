package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.MaxChargeAmountException;
import io.hhplus.tdd.exception.MinChargeAmountException;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserPointService 테스트")
class PointServiceTest {

    final long userId = 1L;
    final long now = System.currentTimeMillis();

    PointService pointService;
    @Mock
    UserPointTable userPointTable;
    @Mock
    PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
        // 테스트 상태 초기화 (Repeatable 보장하기 위해 분리하였다.)
        pointService = new PointService(userPointTable, pointHistoryTable);
    }

    // TODO 테스트 명 영어로 바꾸기

    @Test
    @DisplayName("포인트 조회 - 성공")
    void getPoint_success() {
        // Given
        UserPoint expectedUserPoint = new UserPoint(userId, 1000L, now);
        when(userPointTable.selectById(userId)).thenReturn(expectedUserPoint);

        // When
        UserPoint result = pointService.getPoint(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(1000L);
    }

    // 히스토리 저장 여부는 charge_saveHistory_success 에서 검증한다.
    @Test
    @DisplayName("포인트 충전 - 성공")
    void charge_success() {
        // Given
        long chargeAmount = 1000L;
        UserPoint beforeUserPoint = new UserPoint(userId, 0L, now);
        UserPoint afterUserPoint = beforeUserPoint.add(chargeAmount);
        when(userPointTable.selectById(userId)).thenReturn(beforeUserPoint);
        when(userPointTable.insertOrUpdate(userId, afterUserPoint.point())).thenReturn(afterUserPoint);

        // When
        UserPoint updated = pointService.charge(userId, chargeAmount);

        // Then
        assertThat(updated.point()).isEqualTo(beforeUserPoint.point() + chargeAmount);
    }

    @Test
    @DisplayName("포인트 충전 시 히스토리 저장 호출 - 성공")
    void charge_saveHistory_success() {
        // Given
        long chargeAmount = 1000L;

        UserPoint beforeUserPoint = new UserPoint(userId, 0L, now);
        UserPoint afterUserPoint = beforeUserPoint.add(chargeAmount);
        when(userPointTable.selectById(userId)).thenReturn(beforeUserPoint);
        when(userPointTable.insertOrUpdate(userId, afterUserPoint.point())).thenReturn(afterUserPoint);
        // When
        pointService.charge(userId, chargeAmount);

        // Then
        verify(pointHistoryTable).insert(
                eq(userId),
                eq(chargeAmount),
                eq(TransactionType.CHARGE),
                anyLong()
        );
    }

    @Test
    @DisplayName("포인트 충전 - 음수 입력 시 예외 발생")
    void charge_withNegativeAmount_throwsException() {
        // Given
        long invalidChargeAmount = -1000L;

        // When & Then
        assertThatThrownBy(() -> pointService.charge(userId, invalidChargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("음수");
    }

    @Test
    @DisplayName("포인트 충전 - 최소 금액 미만 시 예외 발생")
    void charge_withAmountBelowMin_throwsException() {
        // given
        long chargeAmount = 50L;  // 최소 제한 100원 미만

        // when & then
        assertThatThrownBy(() -> pointService.charge(userId, chargeAmount))
                .isInstanceOf(MinChargeAmountException.class);
    }

    @Test
    @DisplayName("포인트 충전 - 최대 금액 초과 시 예외 발생")
    void charge_withAmountAboveMax_throwsException() {
        // given
        long chargeAmount = 1_000_001L;  // 최대 제한 1,000,000원 초과

        // when & then
        assertThatThrownBy(() -> pointService.charge(userId, chargeAmount))
                .isInstanceOf(MaxChargeAmountException.class);
    }

    // 히스토리 저장 여부는 use_savesHistory_success 에서 검증한다.
    @Test
    @DisplayName("포인트 사용 - 성공")
    void use_success() {
        // Given
        long useAmount = 300L;
        UserPoint beforeUserPoint = new UserPoint(userId, 1000L, now);
        UserPoint afterUserPoint = new UserPoint(userId, 700L, now);
        when(userPointTable.selectById(userId)).thenReturn(beforeUserPoint);
        when(userPointTable.insertOrUpdate(userId, 700L)).thenReturn(afterUserPoint);

        // When
        UserPoint result = pointService.use(userId, useAmount);

        // Then
        assertThat(result.point()).isEqualTo(beforeUserPoint.point() - useAmount);
    }

    @Test
    @DisplayName("포인트 사용 시 히스토리 저장 호출 - 성공")
    void use_savesHistory_success() {
        // Given
        long useAmount = 200L;
        when(userPointTable.selectById(userId)).thenReturn(new UserPoint(userId, 1000L, now));
        when(userPointTable.insertOrUpdate(userId, 800L)).thenReturn(new UserPoint(userId, 800L, now));

        // When
        pointService.use(userId, useAmount);

        // Then
        verify(pointHistoryTable).insert(
                eq(userId),
                eq(useAmount),
                eq(TransactionType.USE),
                anyLong()
        );
    }

    @Test
    @DisplayName("포인트 사용 - 잔액 부족 시 예외 발생")
    void use_withInsufficientBalance_throwsException() {
        // Given
        long useAmount = 9999L;
        when(userPointTable.selectById(userId)).thenReturn(new UserPoint(userId, 1000L, now));

        // When & Then
        assertThatThrownBy(() -> pointService.use(userId, useAmount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("잔액이 부족합니다.");
    }

    @Test
    @DisplayName("포인트 사용 - 음수 입력 시 예외 발생")
    void use_withNegativeAmount_throwsException() {
        // Given
        long invalidUseAmount = -100L;

        // When & Then
        assertThatThrownBy(() -> pointService.use(userId, invalidUseAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용 금액은 음수일 수 없습니다.");
    }

    @Test
    @DisplayName("포인트 거래 내역 조회 - 성공")
    void getHistories_success() {
        // Given
        List<PointHistory> expectedHistory = List.of(
                new PointHistory(1L, userId, 300L, TransactionType.CHARGE, now),
                new PointHistory(2L, userId, 200L, TransactionType.USE, now)
        );
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(expectedHistory);

        // When
        List<PointHistory> result = pointService.getHistories(userId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(PointHistory::type)
                .containsExactly(TransactionType.CHARGE, TransactionType.USE);
        assertThat(result)
                .extracting(PointHistory::amount)
                .containsExactly(300L, 200L);
    }

    @Test
    @DisplayName("포인트 충전 - 최대 잔고 초과 시 예외 발생")
    void charge_exceedsMaxBalance_throwsException() {
        // Given
        long currentPoint = 9_900_000L;
        long chargeAmount = 200_000L;
        when(userPointTable.selectById(userId)).thenReturn(new UserPoint(userId, currentPoint, now));

        // When & Then
        assertThatThrownBy(() -> pointService.charge(userId, chargeAmount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("최대 잔고는 10,000,000 포인트를 초과할 수 없습니다.");
        // userPointTable.insertOrUpdate()가 호출되지 않음을 명시적으로 검증 (방어코드)
        verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
    }
}
