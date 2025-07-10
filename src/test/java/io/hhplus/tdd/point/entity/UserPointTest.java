package io.hhplus.tdd.point.entity;

import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserPointTest")
public class UserPointTest {

    final long userId = 1L;
    final long now = System.currentTimeMillis();

    @Test
    @DisplayName("empty()는 id에 해당하는 0포인트 UserPoint 객체를 생성한다")
    void empty_returnsUserPointWithZeroPoint() {
        // Given
            // final long userId = 1L;로 선언되어 있는 값 사용
        // When
        UserPoint userPoint = UserPoint.empty(userId);

        // Then
        assertThat(userPoint.id()).isEqualTo(userId);
        assertThat(userPoint.point()).isZero();
    }

    @Test
    @DisplayName("같은 값으로 생성된 UserPoint 객체는 equals()가 true를 반환한다")
    void equals_sameFieldValues_areEqual() {
        // Given
        long point = 1000L;

        UserPoint up1 = new UserPoint(userId, point, now);
        UserPoint up2 = new UserPoint(userId, point, now);

        // Then
        assertThat(up1).isEqualTo(up2);
        assertThat(up1.hashCode()).isEqualTo(up2.hashCode());
    }

    @Test
    @DisplayName("다른 값으로 생성된 UserPoint 객체는 equals()가 false를 반환한다")
    void equals_differentFieldValues_areNotEqual() {
        // Given
        UserPoint up1 = new UserPoint(userId, 1000L, now);
        UserPoint up2 = new UserPoint(userId, 2000L, now);

        // Then
        assertThat(up1).isNotEqualTo(up2);
    }

    @Test
    @DisplayName("음수 포인트로 UserPoint 생성 시 IllegalArgumentException 발생")
    void createUserPointWithNegativePoint_ThrowsException() {
        // Given
        long negativePoint = -1L;

        // When & Then
        assertThatThrownBy(() -> new UserPoint(userId, negativePoint, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("포인트는 음수일 수 없습니다.");
    }

    @Test
    @DisplayName("최대 잔고를 초과하여 UserPoint 생성 시 IllegalArgumentException 발생")
    void createUserPointWithExceedMaxPoint_ThrowsException() {
        // Given
        long exceedMaxPoint = 10_000_001L; // 최대 잔고 초과

        // When & Then
        assertThatThrownBy(() -> new UserPoint(userId, exceedMaxPoint, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최대 잔고는 10,000,000 포인트 입니다.");
    }

    @Test
    @DisplayName("UserPoint의 JSON 직렬화 및 역직렬화가 성공한다")
    void json_serializationDeserialization() throws Exception {
        // Given
        UserPoint original = new UserPoint(userId, 500L, now);

        // When
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(original);
        UserPoint deserialized = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, UserPoint.class);

        // Then
        assertThat(deserialized).isEqualTo(original);
    }
}
