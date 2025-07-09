package io.hhplus.tdd.point.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PointHistory 테스트")
public class PointHistoryTest {
    final long id = 1L;
    final long userId = 100L;
    final long now = System.currentTimeMillis();

    @Test
    @DisplayName("같은 값으로 생성된 PointHistory 객체는 equals()가 true를 반환한다")
    void equals_sameFieldValues_areEqual() {
        // Given
        PointHistory ph1 = new PointHistory(id, userId, 500L, TransactionType.CHARGE, now);
        PointHistory ph2 = new PointHistory(id, userId, 500L, TransactionType.CHARGE, now);

        // Then
        assertThat(ph1).isEqualTo(ph2);
        assertThat(ph1.hashCode()).isEqualTo(ph2.hashCode());
    }

    @Test
    @DisplayName("다른 값으로 생성된 PointHistory 객체는 equals()가 false를 반환한다")
    void equals_differentFieldValues_areNotEqual() {
        // Given
        PointHistory ph1 = new PointHistory(id, userId, 500L, TransactionType.CHARGE, now);
        PointHistory ph2 = new PointHistory(id, userId, 600L, TransactionType.USE, now);

        // Then
        assertThat(ph1).isNotEqualTo(ph2);
    }

    @Test
    @DisplayName("음수 amount로 PointHistory 생성 시 IllegalArgumentException 발생")
    void createPointHistoryWithNegativeAmount_ThrowsException() {
        // Given
        long negativeAmount = -100L;

        // When & Then
        assertThatThrownBy(() -> new PointHistory(id, userId, negativeAmount, TransactionType.CHARGE, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("금액은 음수일 수 없습니다.");
    }

    @Test
    @DisplayName("PointHistory의 JSON 직렬화 및 역직렬화가 성공한다")
    void json_serializationDeserialization() throws Exception {
        // Given
        PointHistory original = new PointHistory(id, userId, 1000L, TransactionType.USE, now);

        ObjectMapper objectMapper = new ObjectMapper();

        // When
        String json = objectMapper.writeValueAsString(original);
        PointHistory deserialized = objectMapper.readValue(json, PointHistory.class);

        // Then
        assertThat(deserialized).isEqualTo(original);
    }
}
