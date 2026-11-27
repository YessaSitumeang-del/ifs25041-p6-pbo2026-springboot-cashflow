package org.delcom.ifs25041springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SortOptionTest {

    private static final LocalDateTime OLDER = LocalDateTime.of(2026, 1, 1, 8, 0);
    private static final LocalDateTime NEWER = LocalDateTime.of(2026, 1, 2, 8, 0);

    private static Transaction tx(int id, String description, long amount, LocalDateTime at) {
        return new Transaction(id, 1, description, amount, TransactionType.EXPENSE, null, at);
    }

    private static List<Integer> idsSortedBy(SortOption option, List<Transaction> input) {
        List<Transaction> copy = new ArrayList<>(input);
        copy.sort(option.comparator());
        return copy.stream().map(Transaction::getId).toList();
    }

    @Test
    void newestAndOldestFirstUseCreatedAtThenId() {
        List<Transaction> input = List.of(tx(1, "lama", 1, OLDER), tx(2, "baru", 1, NEWER), tx(3, "baru2", 1, NEWER));

        assertEquals(List.of(3, 2, 1), idsSortedBy(SortOption.NEWEST_FIRST, input));
        assertEquals(List.of(1, 2, 3), idsSortedBy(SortOption.OLDEST_FIRST, input));
    }

    @Test
    void amountOptionsBreakTiesWithNewestId() {
        List<Transaction> input = List.of(tx(1, "a", 100, OLDER), tx(2, "b", 500, OLDER), tx(3, "c", 100, OLDER));

        assertEquals(List.of(2, 3, 1), idsSortedBy(SortOption.AMOUNT_DESC, input));
        assertEquals(List.of(3, 1, 2), idsSortedBy(SortOption.AMOUNT_ASC, input));
    }

    @Test
    void descriptionOptionsIgnoreCase() {
        List<Transaction> input = List.of(tx(1, "beta", 1, OLDER), tx(2, "Alpha", 1, OLDER));

        assertEquals(List.of(2, 1), idsSortedBy(SortOption.DESCRIPTION_ASC, input));
        assertEquals(List.of(1, 2), idsSortedBy(SortOption.DESCRIPTION_DESC, input));
    }

    @Test
    void everyOptionHasLabelAndAtLeastFourOptionsExist() {
        assertTrue(SortOption.values().length >= 4);
        for (SortOption option : SortOption.values()) {
            assertTrue(!option.label().isBlank());
        }
        assertEquals("Terbaru", SortOption.NEWEST_FIRST.label());
        assertEquals("Terlama", SortOption.OLDEST_FIRST.label());
        assertEquals("Jumlah terbesar", SortOption.AMOUNT_DESC.label());
        assertEquals("Jumlah terkecil", SortOption.AMOUNT_ASC.label());
        assertEquals("Keterangan A-Z", SortOption.DESCRIPTION_ASC.label());
        assertEquals("Keterangan Z-A", SortOption.DESCRIPTION_DESC.label());
    }
}
