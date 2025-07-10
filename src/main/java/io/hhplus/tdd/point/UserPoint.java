package io.hhplus.tdd.point;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {
    public static final long MAX_BALANCE = 10_000_000L;

    public UserPoint {
        validatePoint(point);
    }


    public UserPoint add(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("충전 금액은 음수일 수 없습니다.");
        }

        long newPoint = this.point + amount;
        validatePoint(newPoint);

        return new UserPoint(this.id, newPoint, System.currentTimeMillis());
    }

    public UserPoint subtract(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("사용 금액은 음수일 수 없습니다.");
        }
        if (amount > this.point) {
            throw new IllegalStateException("잔액이 부족합니다.");
        }

        long newPoint = this.point - amount;
        return new UserPoint(this.id, newPoint, System.currentTimeMillis());
    }

    private static void validatePoint(long point) {
        if (point < 0) {
            throw new IllegalArgumentException("포인트는 음수일 수 없습니다.");
        }
        if (point > MAX_BALANCE) {
            throw new IllegalArgumentException("최대 잔고는 10,000,000 포인트 입니다.");
        }
    }

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }
}
