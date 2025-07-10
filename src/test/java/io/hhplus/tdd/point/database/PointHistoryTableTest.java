package io.hhplus.tdd.point.database;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PointHistoryTable 단위 테스트")
public class PointHistoryTableTest {
    private PointHistoryTable table;

    @BeforeEach
    void setUp() {
        table = new PointHistoryTable();
    }

    @Test
    @DisplayName("insert 후 selectAllByUserId 시 정확한 히스토리를 반환한다")
    void insertHistory_thenRetrieveByUserId() {
        // given
        long userId = 1L;
        long amount = 300L;
        TransactionType type = TransactionType.CHARGE;

        PointHistory inserted = table.insert(userId, amount, type, System.currentTimeMillis());

        // when
        List<PointHistory> result = table.selectAllByUserId(userId);

        // then
        assertThat(result).containsExactly(inserted);
    }

    @Test
    @DisplayName("동일 사용자 히스토리는 순서 보장되며 여러 개 저장 가능")
    void multipleInserts_thenAllReturnedInOrder() {
        // given
        long userId = 2L;
        PointHistory h1 = table.insert(userId, 100L, TransactionType.CHARGE, System.currentTimeMillis());
        PointHistory h2 = table.insert(userId, 200L, TransactionType.USE, System.currentTimeMillis());

        // when
        List<PointHistory> result = table.selectAllByUserId(userId);

        // then
        assertThat(result).containsExactly(h1, h2);
    }

    @Test
    @DisplayName("사용자별 히스토리는 격리되어 저장된다")
    void multipleUsers_thenHistoriesAreIsolated() {
        // given
        PointHistory user1 = table.insert(1L, 1000L, TransactionType.CHARGE, System.currentTimeMillis());
        PointHistory user2 = table.insert(2L, 500L, TransactionType.USE, System.currentTimeMillis());

        // then
        assertThat(table.selectAllByUserId(1L)).containsExactly(user1);
        assertThat(table.selectAllByUserId(2L)).containsExactly(user2);
    }

    @Test
    @DisplayName("히스토리가 없는 사용자는 빈 리스트 반환")
    void noHistoryForUser_thenReturnEmptyList() {
        // when
        List<PointHistory> result = table.selectAllByUserId(999L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("insert 시 ID는 자동 증가한다")
    void insert_thenAutoIncrementId() {
        // given
        PointHistory h1 = table.insert(1L, 100L, TransactionType.CHARGE, System.currentTimeMillis());
        PointHistory h2 = table.insert(1L, 200L, TransactionType.USE, System.currentTimeMillis());

        // then
        assertThat(h2.id()).isEqualTo(h1.id() + 1);
    }
}
