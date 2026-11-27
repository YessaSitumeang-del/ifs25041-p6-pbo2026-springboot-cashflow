package org.delcom.ifs25041springbootcashflow.usecase;

import org.delcom.ifs25041springbootcashflow.adapter.repository.TransactionRepository;
import org.delcom.ifs25041springbootcashflow.domain.entity.FinanceSummary;
import org.delcom.ifs25041springbootcashflow.domain.entity.Role;
import org.delcom.ifs25041springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25041springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25041springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25041springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25041springbootcashflow.domain.entity.User;
import org.delcom.ifs25041springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25041springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceUseCaseTest {

    private TransactionRepository repository;
    private FinanceUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new TransactionRepository();
        useCase = new FinanceUseCase(repository);
    }

    private static List<String> descriptions(List<Transaction> transactions) {
        return transactions.stream().map(Transaction::getDescription).toList();
    }

    private void seed() throws Exception {
        useCase.addTransaction(1, "Gaji", 5_000_000, TransactionType.INCOME, null);
        useCase.addTransaction(1, "Makan siang", 25_000, TransactionType.EXPENSE, null);
        useCase.addTransaction(1, "Bonus", 100_000, TransactionType.INCOME, "proofs/b.png");
        useCase.addTransaction(2, "Milik orang lain", 999, TransactionType.EXPENSE, null);
    }

    // ---------------------------------------------------------------- daftar

    @Test
    void defaultListIsNewestFirstAndOnlyOwnData() throws Exception {
        seed();
        assertEquals(List.of("Bonus", "Makan siang", "Gaji"), descriptions(useCase.getTransactionsByUser(1)));
        assertEquals(List.of("Milik orang lain"), descriptions(useCase.getTransactionsByUser(2)));
        assertTrue(useCase.getTransactionsByUser(99).isEmpty());
    }

    @Test
    void listSearchesDescriptionCaseInsensitively() throws Exception {
        seed();
        assertEquals(List.of("Makan siang"),
                descriptions(useCase.listTransactions(1, "  MAKAN ", TransactionTypeFilter.ALL, SortOption.NEWEST_FIRST)));
        assertEquals(3, useCase.listTransactions(1, "   ", TransactionTypeFilter.ALL, SortOption.NEWEST_FIRST).size());
        assertEquals(3, useCase.listTransactions(1, null, TransactionTypeFilter.ALL, SortOption.NEWEST_FIRST).size());
    }

    @Test
    void listFiltersByType() throws Exception {
        seed();
        assertEquals(List.of("Bonus", "Gaji"),
                descriptions(useCase.listTransactions(1, null, TransactionTypeFilter.INCOME, SortOption.NEWEST_FIRST)));
        assertEquals(List.of("Makan siang"),
                descriptions(useCase.listTransactions(1, null, TransactionTypeFilter.EXPENSE, SortOption.NEWEST_FIRST)));
    }

    @Test
    void listSortsAndFallsBackToDefaultsForNull() throws Exception {
        seed();
        assertEquals(List.of("Gaji", "Makan siang", "Bonus"),
                descriptions(useCase.listTransactions(1, null, TransactionTypeFilter.ALL, SortOption.OLDEST_FIRST)));
        assertEquals(List.of("Gaji", "Bonus", "Makan siang"),
                descriptions(useCase.listTransactions(1, null, TransactionTypeFilter.ALL, SortOption.AMOUNT_DESC)));
        assertEquals(List.of("Bonus", "Makan siang", "Gaji"),
                descriptions(useCase.listTransactions(1, null, null, null)));
    }

    // ---------------------------------------------------------------- admin read-only

    @Test
    void ownerAndAdminMayViewButOthersMayNot() throws Exception {
        seed();
        User owner = new User(1, "a", "h", "A", Role.USER, null);
        User admin = new User(9, "root", "h", "Root", Role.ADMIN, null);
        User stranger = new User(2, "b", "h", "B", Role.USER, null);

        assertEquals(3, useCase.getTransactionsOf(owner, 1).size());
        assertEquals(3, useCase.getTransactionsOf(admin, 1).size());
        assertThrows(ValidationException.class, () -> useCase.getTransactionsOf(stranger, 1));
    }

    // ---------------------------------------------------------------- saldo

    @Test
    void summaryAndBalanceAddIncomeAndSubtractExpense() throws Exception {
        seed();
        FinanceSummary summary = useCase.getSummary(1);
        assertEquals(3, summary.count());
        assertEquals(5_100_000L, summary.income());
        assertEquals(25_000L, summary.expense());
        assertEquals(5_075_000L, useCase.getBalance(1));
        assertEquals(-999L, useCase.getBalance(2));
    }

    @Test
    void emptyUserHasZeroBalance() {
        assertEquals(0L, useCase.getBalance(5));
        assertEquals(0, useCase.getSummary(5).count());
    }

    @Test
    void balanceStaysConsistentAfterAddAndRemove() throws Exception {
        Transaction income = useCase.addTransaction(1, "Gaji", 1_000, TransactionType.INCOME, null);
        Transaction expense = useCase.addTransaction(1, "Jajan", 300, TransactionType.EXPENSE, null);
        assertEquals(700L, useCase.getBalance(1));

        useCase.removeTransactionForUser(1, expense.getId());
        assertEquals(1_000L, useCase.getBalance(1));
        useCase.removeTransactionForUser(1, income.getId());
        assertEquals(0L, useCase.getBalance(1));
    }

    // ---------------------------------------------------------------- validasi input

    @Test
    void validateInputReturnsParsedAmount() throws Exception {
        assertEquals(15_000L, useCase.validateInput("Kopi", " 15000 "));
    }

    @Test
    void validateInputRejectsBlankDescription() {
        assertThrows(ValidationException.class, () -> useCase.validateInput(null, "10"));
        assertThrows(ValidationException.class, () -> useCase.validateInput("   ", "10"));
    }

    @Test
    void validateInputRejectsMissingNonNumericAndNonPositiveAmount() {
        assertThrows(ValidationException.class, () -> useCase.validateInput("a", null));
        assertThrows(ValidationException.class, () -> useCase.validateInput("a", "  "));
        assertThrows(ValidationException.class, () -> useCase.validateInput("a", "abc"));
        assertThrows(ValidationException.class, () -> useCase.validateInput("a", "1.000"));
        assertThrows(ValidationException.class, () -> useCase.validateInput("a", "99999999999999999999"));
        assertThrows(ValidationException.class, () -> useCase.validateInput("a", "0"));
        assertThrows(ValidationException.class, () -> useCase.validateInput("a", "-5"));
    }

    // ---------------------------------------------------------------- tambah

    @Test
    void addTransactionTrimsDescriptionAndNormalizesProof() throws Exception {
        Transaction withProof = useCase.addTransaction(1, "  Gaji  ", 10, TransactionType.INCOME, "proofs/x.png");
        assertEquals("Gaji", withProof.getDescription());
        assertEquals("proofs/x.png", withProof.getProofPath());

        assertNull(useCase.addTransaction(1, "a", 10, TransactionType.INCOME, null).getProofPath());
        assertNull(useCase.addTransaction(1, "a", 10, TransactionType.INCOME, "   ").getProofPath());
    }

    @Test
    void addTransactionRejectsInvalidInput() {
        assertThrows(ValidationException.class, () -> useCase.addTransaction(1, "", 10, TransactionType.INCOME, null));
        assertThrows(ValidationException.class, () -> useCase.addTransaction(1, "a", 0, TransactionType.INCOME, null));
        assertThrows(ValidationException.class, () -> useCase.addTransaction(1, "a", -1, TransactionType.INCOME, null));
        assertThrows(ValidationException.class, () -> useCase.addTransaction(1, "a", 10, null, null));
        assertTrue(repository.findAll().isEmpty());
    }

    // ---------------------------------------------------------------- hapus

    @Test
    void removeReturnsTheDeletedTransaction() throws Exception {
        Transaction created = useCase.addTransaction(1, "Gaji", 10, TransactionType.INCOME, "proofs/x.png");
        Transaction removed = useCase.removeTransactionForUser(1, created.getId());

        assertEquals("proofs/x.png", removed.getProofPath());
        assertTrue(repository.findById(created.getId()).isEmpty());
    }

    @Test
    void removeRejectsInvalidId() {
        assertThrows(ValidationException.class, () -> useCase.removeTransactionForUser(1, 0));
        assertThrows(ValidationException.class, () -> useCase.removeTransactionForUser(1, -3));
    }

    @Test
    void removeFailsForUnknownId() {
        assertThrows(EntityNotFoundException.class, () -> useCase.removeTransactionForUser(1, 999));
    }

    @Test
    void removeRefusesOtherUsersTransactionEvenForAdminIds() throws Exception {
        Transaction created = useCase.addTransaction(1, "Gaji", 10, TransactionType.INCOME, null);
        assertThrows(ValidationException.class, () -> useCase.removeTransactionForUser(2, created.getId()));
        assertTrue(repository.findById(created.getId()).isPresent());
    }

    @Test
    void removeReportsNotFoundWhenRowVanishesBetweenLookupAndDelete() throws Exception {
        TransactionRepository racing = new TransactionRepository() {
            @Override
            public boolean deleteById(Integer id) {
                return false;
            }
        };
        FinanceUseCase racingUseCase = new FinanceUseCase(racing);
        Transaction created = racingUseCase.addTransaction(1, "Gaji", 10, TransactionType.INCOME, null);

        assertThrows(EntityNotFoundException.class, () -> racingUseCase.removeTransactionForUser(1, created.getId()));
    }
}
