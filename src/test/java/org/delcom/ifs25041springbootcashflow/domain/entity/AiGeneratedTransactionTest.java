package org.delcom.ifs25041springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiGeneratedTransactionTest {

    @Test
    void trimsDescription() {
        AiGeneratedTransaction item = new AiGeneratedTransaction("  Makan siang ", 25_000, TransactionType.EXPENSE);
        assertEquals("Makan siang", item.description());
        assertEquals(25_000, item.amount());
        assertEquals(TransactionType.EXPENSE, item.type());
    }

    @Test
    void rejectsBlankDescription() {
        assertThrows(IllegalArgumentException.class, () -> new AiGeneratedTransaction(null, 1, TransactionType.INCOME));
        assertThrows(IllegalArgumentException.class, () -> new AiGeneratedTransaction("   ", 1, TransactionType.INCOME));
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () -> new AiGeneratedTransaction("a", 0, TransactionType.INCOME));
        assertThrows(IllegalArgumentException.class, () -> new AiGeneratedTransaction("a", -5, TransactionType.INCOME));
    }

    @Test
    void rejectsMissingType() {
        assertThrows(IllegalArgumentException.class, () -> new AiGeneratedTransaction("a", 1, null));
    }
}
