package org.delcom.ifs25041springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinanceSummaryTest {

    @Test
    void balanceIsIncomeMinusExpense() {
        FinanceSummary summary = new FinanceSummary(3, 5_000_000L, 25_000L);
        assertEquals(3, summary.count());
        assertEquals(4_975_000L, summary.balance());
    }

    @Test
    void balanceCanBeNegative() {
        assertEquals(-100L, new FinanceSummary(1, 0, 100).balance());
    }
}
