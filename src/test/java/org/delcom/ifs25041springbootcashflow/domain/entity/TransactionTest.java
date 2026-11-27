package org.delcom.ifs25041springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionTest {

    @Test
    void exposesAllFields() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 4, 5, 6);
        Transaction transaction = new Transaction(7, 3, "Gaji", 5_000_000L, TransactionType.INCOME, "proofs/a.png", now);

        assertEquals(7, transaction.getId());
        assertEquals(3, transaction.getUserId());
        assertEquals("Gaji", transaction.getDescription());
        assertEquals(5_000_000L, transaction.getAmount());
        assertEquals(TransactionType.INCOME, transaction.getType());
        assertEquals("proofs/a.png", transaction.getProofPath());
        assertEquals(now, transaction.getCreatedAt());
    }

    @Test
    void hasProofOnlyWhenPathIsPresentAndNotBlank() {
        LocalDateTime now = LocalDateTime.now();
        assertTrue(new Transaction(1, 1, "a", 1, TransactionType.EXPENSE, "p.png", now).hasProof());
        assertFalse(new Transaction(1, 1, "a", 1, TransactionType.EXPENSE, null, now).hasProof());
        assertFalse(new Transaction(1, 1, "a", 1, TransactionType.EXPENSE, "  ", now).hasProof());
        assertNull(new Transaction(1, 1, "a", 1, TransactionType.EXPENSE, null, now).getProofPath());
    }
}
