package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.MaxChargeAmountException;
import io.hhplus.tdd.exception.MinChargeAmountException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {


    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public UserPoint charge(long userId, long chargeAmount) {
        if (chargeAmount < 0) {
            throw new IllegalArgumentException("충전 금액은 음수일 수 없습니다");
        }
        if (chargeAmount < 100) {
            throw new MinChargeAmountException();
        }
        if (chargeAmount > 1000000) {
            throw new MaxChargeAmountException();
        }

        UserPoint current = userPointTable.selectById(userId);
        long updatedPoint = current.point() + chargeAmount;

        if (updatedPoint > 10_000_000) {
            throw new IllegalStateException("최대 잔고는 10,000,000 포인트를 초과할 수 없습니다.");
        }

        // UserPoint 업데이트
        UserPoint updated = userPointTable.insertOrUpdate(userId, updatedPoint);

        // 충전 내역 기록
        pointHistoryTable.insert(userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());

        return updated;
    }

    public UserPoint getPoint(long userId) {
        return userPointTable.selectById(userId);
    }

    public List<PointHistory> getHistories(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    public UserPoint use(long userId, long useAmount) {
        if (useAmount < 0) {
            throw new IllegalArgumentException("사용 금액은 음수일 수 없습니다.");
        }

        UserPoint current = userPointTable.selectById(userId);
        if (current.point() < useAmount) {
            throw new IllegalStateException("잔액이 부족합니다.");
        }

        long updatedPoint = current.point() - useAmount;

        UserPoint updated = userPointTable.insertOrUpdate(userId, updatedPoint);
        pointHistoryTable.insert(userId, useAmount, TransactionType.USE, System.currentTimeMillis());

        return updated;
    }
}
