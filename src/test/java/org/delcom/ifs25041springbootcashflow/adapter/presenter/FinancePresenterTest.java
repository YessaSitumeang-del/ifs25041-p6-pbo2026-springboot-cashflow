package org.delcom.ifs25041springbootcashflow.adapter.presenter;

import org.delcom.ifs25041springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25041springbootcashflow.domain.entity.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinancePresenterTest {

    private FinancePresenter presenter;
    private Transaction income;
    private Transaction expense;

    @BeforeEach
    void setUp() {
        presenter = new FinancePresenter();
        LocalDateTime at = LocalDateTime.of(2026, 5, 17, 9, 5);
        income = new Transaction(1, 1, "Gaji", 1_000_000L, TransactionType.INCOME, null, at);
        expense = new Transaction(2, 1, "Makan siang", 25_000L, TransactionType.EXPENSE, null, at);
    }

    @Test
    void formatsMoneyWithThousandsSeparatorAndTwoDecimals() {
        assertEquals("Rp 1,000,000.00", presenter.formatMoney(1_000_000L));
        assertEquals("Rp 0.00", presenter.formatMoney(0));
        assertEquals("Rp 999.00", presenter.formatMoney(999));
        assertEquals("Rp 9,223,372,036,854,775,807.00", presenter.formatMoney(Long.MAX_VALUE));
    }

    @Test
    void formatsNegativeMoneyWithLeadingMinus() {
        assertEquals("-Rp 25,000.00", presenter.formatMoney(-25_000L));
    }

    @Test
    void formatsTransactionLikeTheSpecification() {
        assertEquals("[1] Gaji | Rp 1,000,000.00 | [Pemasukan]", presenter.format(income));
        assertEquals("[2] Makan siang | Rp 25,000.00 | [Pengeluaran]", presenter.format(expense));
    }

    @Test
    void formatsBalanceLikeTheSpecification() {
        assertEquals("Saldo: Rp 4,975,000.00", presenter.formatBalance(4_975_000L));
        assertEquals("Saldo: -Rp 10.00", presenter.formatBalance(-10));
    }

    @Test
    void formatsCreatedAt() {
        assertEquals("17/05/2026 09:05", presenter.formatCreatedAt(income));
    }

    @Test
    void buildsNotificationMessages() {
        assertEquals("Berhasil mencatat transaksi: [1] Gaji | Rp 1,000,000.00 | [Pemasukan]",
                presenter.showAddSuccess(income));
        assertEquals("Berhasil menghapus transaksi: [2] Makan siang | Rp 25,000.00 | [Pengeluaran]",
                presenter.showRemoveSuccess(expense));
        assertEquals("[!] Gagal menghapus transaksi dengan ID: 7.", presenter.showRemoveFailed(7));
        assertEquals("[!] Keterangan tidak boleh kosong", presenter.showValidationError("Keterangan tidak boleh kosong"));
    }
}
