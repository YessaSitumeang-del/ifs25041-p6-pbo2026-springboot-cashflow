package org.delcom.ifs25041springbootcashflow.adapter.repository;

import org.delcom.ifs25041springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25041springbootcashflow.domain.entity.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionRepositoryTest {

    private TransactionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new TransactionRepository();
    }

    @Test
    void savesWithIncrementingIdsAndTimestamp() {
        Transaction first = repository.save(1, "Gaji", 100, TransactionType.INCOME, null);
        Transaction second = repository.save(1, "Makan", 20, TransactionType.EXPENSE, "proofs/a.png");

        assertEquals(1, first.getId());
        assertEquals(2, second.getId());
        assertEquals("proofs/a.png", second.getProofPath());
        assertNotNull(first.getCreatedAt());
    }

    @Test
    void findsByUserAndById() {
        repository.save(1, "a", 1, TransactionType.INCOME, null);
        repository.save(2, "b", 1, TransactionType.INCOME, null);
        repository.save(1, "c", 1, TransactionType.INCOME, null);

        assertEquals(2, repository.findByUserId(1).size());
        assertEquals(1, repository.findByUserId(2).size());
        assertTrue(repository.findByUserId(3).isEmpty());
        assertTrue(repository.findById(2).isPresent());
        assertTrue(repository.findById(99).isEmpty());
        assertEquals(3, repository.findAll().size());
    }

    @Test
    void deleteReportsWhetherSomethingWasRemoved() {
        Transaction saved = repository.save(1, "a", 1, TransactionType.INCOME, null);

        assertTrue(repository.deleteById(saved.getId()));
        assertFalse(repository.deleteById(saved.getId()));
    }

    @Test
    void updateReplacesOnlyTheEntityWithTheSameId() {
        Transaction first = repository.save(1, "a", 1, TransactionType.INCOME, null);
        Transaction second = repository.save(1, "b", 2, TransactionType.EXPENSE, null);

        Transaction changed = new Transaction(first.getId(), 1, "a-baru", 50, TransactionType.EXPENSE,
                "proofs/z.png", LocalDateTime.now());
        repository.update(changed);

        Transaction reloaded = repository.findById(first.getId()).orElseThrow();
        assertEquals("a-baru", reloaded.getDescription());
        assertEquals(50, reloaded.getAmount());
        assertEquals("b", repository.findById(second.getId()).orElseThrow().getDescription());
    }
}
