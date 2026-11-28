package org.delcom.ifs25041springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionTypeFilterTest {

    private final Transaction income = new Transaction(1, 1, "a", 1, TransactionType.INCOME, null, LocalDateTime.now());
    private final Transaction expense = new Transaction(2, 1, "b", 1, TransactionType.EXPENSE, null, LocalDateTime.now());

    @Test
    void allMatchesEverything() {
        assertTrue(TransactionTypeFilter.ALL.matches(income));
        assertTrue(TransactionTypeFilter.ALL.matches(expense));
    }

    @Test
    void incomeMatchesOnlyIncome() {
        assertTrue(TransactionTypeFilter.INCOME.matches(income));
        assertFalse(TransactionTypeFilter.INCOME.matches(expense));
    }

    @Test
    void expenseMatchesOnlyExpense() {
        assertTrue(TransactionTypeFilter.EXPENSE.matches(expense));
        assertFalse(TransactionTypeFilter.EXPENSE.matches(income));
    }

    @Test
    void labels() {
        assertEquals("Semua tipe", TransactionTypeFilter.ALL.label());
        assertEquals("Pemasukan", TransactionTypeFilter.INCOME.label());
        assertEquals("Pengeluaran", TransactionTypeFilter.EXPENSE.label());
    }
}
