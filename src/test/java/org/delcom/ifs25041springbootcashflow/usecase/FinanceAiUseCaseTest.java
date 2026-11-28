package org.delcom.ifs25041springbootcashflow.usecase;

import org.delcom.ifs25041springbootcashflow.adapter.ai.InMemoryFinanceAiService;
import org.delcom.ifs25041springbootcashflow.adapter.repository.TransactionRepository;
import org.delcom.ifs25041springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25041springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25041springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25041springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25041springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceAiUseCaseTest {

    private InMemoryFinanceAiService aiService;
    private TransactionRepository repository;
    private FinanceAiUseCase useCase;

    @BeforeEach
    void setUp() {
        aiService = new InMemoryFinanceAiService();
        repository = new TransactionRepository();
        useCase = new FinanceAiUseCase(aiService, repository);
    }

    @Test
    void generateRejectsBlankPrompt() {
        assertThrows(ValidationException.class, () -> useCase.generateTransactions(null));
        assertThrows(ValidationException.class, () -> useCase.generateTransactions("   "));
    }

    @Test
    void generateDelegatesToServiceWithTrimmedPrompt() throws Exception {
        List<AiGeneratedTransaction> items = useCase.generateTransactions("  gajian  ");
        assertEquals(2, items.size());
        assertEquals("Pemasukan AI: gajian", items.get(0).description());
    }

    @Test
    void generatePropagatesServiceFailure() {
        aiService.setSimulateFailure(true);
        assertThrows(DomainException.class, () -> useCase.generateTransactions("apa saja"));
    }

    @Test
    void saveStoresEveryItemForTheUserWithoutProof() throws Exception {
        List<AiGeneratedTransaction> items = List.of(
                new AiGeneratedTransaction("Gaji", 5_000_000, TransactionType.INCOME),
                new AiGeneratedTransaction("Makan", 25_000, TransactionType.EXPENSE));

        List<Transaction> saved = useCase.saveGeneratedTransactions(7, items);

        assertEquals(2, saved.size());
        assertEquals(2, repository.findByUserId(7).size());
        assertTrue(repository.findByUserId(8).isEmpty());
        assertNull(saved.get(0).getProofPath());
        assertEquals(TransactionType.EXPENSE, saved.get(1).getType());
    }

    @Test
    void saveRejectsEmptyOrNullList() {
        assertThrows(ValidationException.class, () -> useCase.saveGeneratedTransactions(1, List.of()));
        assertThrows(ValidationException.class, () -> useCase.saveGeneratedTransactions(1, null));
    }
}
