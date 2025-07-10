package io.hhplus.tdd.point.database;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.entity.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserPointTable 단위 테스트")
public class UserPointTableTest {

    private UserPointTable table;

    @BeforeEach
    void setUp() {
        table = new UserPointTable();
    }

    @Test
    @DisplayName("신규 사용자의 포인트 조회 시 기본 값 반환")
    void givenNewUser_whenSelect_thenReturnEmptyUserPoint() {
        // given
        Long userId = 100L;

        // when
        UserPoint result = table.selectById(userId);

        // then
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isZero();
    }

    @Test
    @DisplayName("포인트 삽입 후 조회 시 동일한 UserPoint 반환")
    void givenInsertedUserPoint_whenSelect_thenMatchInserted() {
        // given
        Long userId = 101L;
        long amount = 5000L;
        UserPoint inserted = table.insertOrUpdate(userId, amount);

        // when
        UserPoint result = table.selectById(userId);

        // then
        assertThat(result).isEqualTo(inserted);
    }

    @Test
    @DisplayName("동일 사용자 ID에 대해 포인트가 갱신된다")
    void givenExistingUser_whenUpdate_thenPointIsUpdated() {
        // given
        Long userId = 102L;
        table.insertOrUpdate(userId, 1000L);

        // when
        UserPoint updated = table.insertOrUpdate(userId, 7000L);

        // then
        assertThat(updated.point()).isEqualTo(7000L);
    }

    @Test
    @DisplayName("다수의 사용자 정보를 정확히 저장한다")
    void givenMultipleUsers_whenInsertOrUpdate_thenAllManagedIndependently() {
        // given
        Long id1 = 1L, id2 = 2L, id3 = 3L;

        table.insertOrUpdate(id1, 100L);
        table.insertOrUpdate(id2, 200L);
        table.insertOrUpdate(id3, 300L);

        // then
        assertThat(table.selectById(id1).point()).isEqualTo(100L);
        assertThat(table.selectById(id2).point()).isEqualTo(200L);
        assertThat(table.selectById(id3).point()).isEqualTo(300L);
    }

}
