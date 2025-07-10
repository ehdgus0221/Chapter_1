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

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }
}
