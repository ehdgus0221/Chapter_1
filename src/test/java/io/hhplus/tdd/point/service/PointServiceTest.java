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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {


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

    // TODO 테스트 명 통일성 있게 하기

    // === 포인트 조회 ===
    @Test
    void 포인트_조회_시_정상적으로_포인트를_반환한다() {
        // Given
        long userId = 2L;
        UserPoint expectedUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(expectedUserPoint);

        // When
        UserPoint result = pointService.getPoint(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(1000L);
    }

    // === 포인트 충전 ===
    @Test
    void 충전하면_포인트가_증가한다() {
        // Given
        long userId = 1L;
        long chargeAmount = 1000L;
        UserPoint beforeUserPoint = new UserPoint(userId, 0L, System.currentTimeMillis());
        UserPoint afterUserPoint = new UserPoint(userId, chargeAmount, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(beforeUserPoint);
        when(userPointTable.insertOrUpdate(userId, chargeAmount)).thenReturn(afterUserPoint);

        // When
        UserPoint updated = pointService.charge(userId, chargeAmount);

        // Then
        assertThat(updated.point()).isEqualTo(chargeAmount);
    }

    @Test
    void 충전하면_히스토리_저장이_호출된다() {
        // Given
        long userId = 1L;
        long chargeAmount = 1000L;
        when(userPointTable.selectById(userId)).thenReturn(new UserPoint(userId, 0L, System.currentTimeMillis()));
        when(userPointTable.insertOrUpdate(userId, chargeAmount)).thenReturn(new UserPoint(userId, chargeAmount, System.currentTimeMillis()));

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
    void 충전할_금액이_음수이면_예외를_던진다() {
        // Given
        long userId = 1L;
        long invalidChargeAmount = -1000L;

        // When & Then
        assertThatThrownBy(() -> pointService.charge(userId, invalidChargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("음수");
    }

    @Test
    void 충전_금액이_최소_제한_미만이면_예외가_발생한다() {
        // given
        long userId = 1L;
        long chargeAmount = 50L;  // 최소 제한 100원 미만

        // when & then
        assertThatThrownBy(() -> pointService.charge(userId, chargeAmount))
                .isInstanceOf(MinChargeAmountException.class);
    }

    @Test
    void 충전_금액이_최대_제한_초과하면_예외가_발생한다() {
        // given
        long userId = 1L;
        long chargeAmount = 1_000_001L;  // 최대 제한 1,000,000원 초과

        // when & then
        assertThatThrownBy(() -> pointService.charge(userId, chargeAmount))
                .isInstanceOf(MaxChargeAmountException.class);
    }

    // === 포인트 사용 ===
    @Test
    void 사용하면_포인트가_차감된다() {
        // Given
        long userId = 2L;
        long useAmount = 300L;
        UserPoint beforeUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        UserPoint afterUserPoint = new UserPoint(userId, 700L, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(beforeUserPoint);
        when(userPointTable.insertOrUpdate(userId, 700L)).thenReturn(afterUserPoint);

        // When
        UserPoint result = pointService.use(userId, useAmount);

        // Then
        assertThat(result.point()).isEqualTo(700L);
    }

    @Test
    void 사용하면_히스토리_저장이_호출된다() {
        // Given
        long userId = 2L;
        long useAmount = 200L;
        when(userPointTable.selectById(userId)).thenReturn(new UserPoint(userId, 1000L, System.currentTimeMillis()));
        when(userPointTable.insertOrUpdate(userId, 800L)).thenReturn(new UserPoint(userId, 800L, System.currentTimeMillis()));

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
    void 사용금액이_잔액보다_크면_예외를_던진다() {
        // Given
        long userId = 1L;
        long useAmount = 9999L;
        when(userPointTable.selectById(userId)).thenReturn(new UserPoint(userId, 1000L, System.currentTimeMillis()));

        // When & Then
        assertThatThrownBy(() -> pointService.use(userId, useAmount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("잔액이 부족");
    }

    @Test
    void 사용금액이_음수면_예외를_던진다() {
        // Given
        long userId = 1L;
        long invalidUseAmount = -100L;

        // When & Then
        assertThatThrownBy(() -> pointService.use(userId, invalidUseAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("음수");
    }

    // === 히스토리 조회 ===
    @Test
    void 히스토리_조회_시_정상적으로_내역을_반환한다() {
        // Given
        long userId = 1L;
        List<PointHistory> expectedHistory = List.of(
                new PointHistory(1L, userId, 300L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, userId, 200L, TransactionType.USE, System.currentTimeMillis())
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

}
