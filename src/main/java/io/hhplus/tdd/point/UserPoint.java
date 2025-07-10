package io.hhplus.tdd.point;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {
    public UserPoint {
        if (point < 0) {
            throw new IllegalArgumentException("포인트는 음수일 수 없습니다.");
        }
        if (point > 10_000_000) {
            throw new IllegalArgumentException("최대 잔고는 10,000,000 포인트 입니다.");
        }
    }

    public UserPoint add(long amount) {
        long newPoint = this.point + amount;

        if (newPoint > 10_000_000) {
            throw new IllegalStateException("최대 잔고는 10,000,000 포인트를 초과할 수 없습니다.");
        }

        return new UserPoint(this.id, newPoint, System.currentTimeMillis());
    }

    public UserPoint subtract(long amount) {
        if (amount > this.point) throw new IllegalStateException("잔액 부족");
        return new UserPoint(id, this.point - amount, System.currentTimeMillis());
    }

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }
}
