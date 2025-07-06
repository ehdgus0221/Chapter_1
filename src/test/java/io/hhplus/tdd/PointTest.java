package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PointTest {

    @Autowired
    PointService pointService;
    @Mock
    UserPointTable userPointTable;
    @Mock
    PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
        // 테스트 상태 초기화 (Repeatable 보장하기 위해 분리하였다.)
        userPointTable.insertOrUpdate(1L, 0L);
        userPointTable.insertOrUpdate(2L, 1000L);
    }

    @Test
    void 포인트를_0에서_1000으로_정상_충전한다() {
        // given
        long userId = 1L;
        long chargeAmount = 1000L;

        // when
        UserPoint updated = pointService.charge(userId, chargeAmount);

        // then
        assertThat(updated.id()).isEqualTo(userId);
        assertThat(updated.point()).isEqualTo(1000L);
    }

    @Test
    void 기존_포인트에_충전금액이_누적된다() {
        // given
        long userId = 2L;
        long chargeAmount = 1000L;

        // when
        UserPoint updated = pointService.charge(userId, chargeAmount);

        // then
        assertThat(updated.point()).isEqualTo(2000L);
    }

    @Test
    void 충전_시_히스토리가_저장된다() {
        // given
        long userId = 1L;
        long chargeAmount = 500L;

        // when
        pointService.charge(userId, chargeAmount);

        // then
        List<PointHistory> historyList = pointHistoryTable.selectAllByUserId(userId);

        assertThat(historyList).isNotEmpty();
        PointHistory last = historyList.get(historyList.size() - 1);
        assertThat(last.userId()).isEqualTo(userId);
        assertThat(last.amount()).isEqualTo(chargeAmount);
        assertThat(last.type()).isEqualTo(TransactionType.CHARGE);
    }

    @Test
    void 음수_금액_충전_시_예외를_던진다() {
        // given
        long userId = 1L;
        long chargeAmount = -1000L;

        // when & then
        assertThatThrownBy(() -> pointService.charge(userId, chargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("음수");
    }

    @Test
    void 포인트를_조회한다() {
        UserPoint result = pointService.getPoint(2L);
        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.point()).isEqualTo(1000L);
    }

    @Test
    void 포인트를_사용하면_차감된다() {
        long userId = 2L;
        UserPoint result = pointService.use(userId, 300L);

        assertThat(result.point()).isEqualTo(700L);
    }

    @Test
    void 포인트_사용_시_히스토리가_저장된다() {
        long userId = 2L;
        pointService.use(userId, 200L);

        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(userId);
        assertThat(histories).anySatisfy(h -> {
            assertThat(h.userId()).isEqualTo(userId);
            assertThat(h.amount()).isEqualTo(200L);
            assertThat(h.type()).isEqualTo(TransactionType.USE);
        });
    }

    @Test
    void 사용금액이_잔액보다_크면_예외발생() {
        assertThatThrownBy(() -> pointService.use(1L, 9999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("잔액이 부족");
    }

    @Test
    void 음수_금액_사용_시_예외발생() {
        assertThatThrownBy(() -> pointService.use(1L, -100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("음수");
    }

    @Test
    void 히스토리_조회_결과를_확인한다() {
        pointService.charge(1L, 300L);
        pointService.use(1L, 200L);

        List<PointHistory> result = pointService.getHistories(1L);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).type()).isIn(TransactionType.CHARGE, TransactionType.USE);
    }

}
