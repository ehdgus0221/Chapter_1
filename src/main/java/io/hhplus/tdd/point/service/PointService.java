package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.MaxChargeAmountException;
import io.hhplus.tdd.exception.MinChargeAmountException;
import io.hhplus.tdd.point.entity.PointHistory;
import io.hhplus.tdd.point.entity.TransactionType;
import io.hhplus.tdd.point.entity.UserPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PointService {


    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    // 동시성 제어용 사용자별 락 객체 저장소
    private final Map<Long, Object> locks = new ConcurrentHashMap<>();

    private Object getLock(long userId) {
        return locks.computeIfAbsent(userId, id -> new Object());
    }

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

        synchronized (getLock(userId)) {
            UserPoint current = userPointTable.selectById(userId);
            UserPoint updated = current.add(chargeAmount);

            if (updated.point() > 10_000_000) {
                throw new IllegalStateException("최대 잔고는 10,000,000 포인트를 초과할 수 없습니다.");
            }

            userPointTable.insertOrUpdate(userId, updated.point());
            pointHistoryTable.insert(userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());

            return updated;
        }
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

        synchronized (getLock(userId)) {
            UserPoint current = userPointTable.selectById(userId);

            if (current.point() < useAmount) {
                throw new IllegalStateException("잔액이 부족합니다.");
            }

            UserPoint updated = current.subtract(useAmount);

            userPointTable.insertOrUpdate(userId, updated.point());
            pointHistoryTable.insert(userId, useAmount, TransactionType.USE, System.currentTimeMillis());

            return updated;
        }
    }
}
