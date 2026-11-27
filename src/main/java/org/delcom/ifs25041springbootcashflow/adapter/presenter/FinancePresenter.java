package org.delcom.ifs25041springbootcashflow.adapter.presenter;

import org.delcom.ifs25041springbootcashflow.domain.entity.Transaction;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Presenter yang memformat data dari use case menjadi teks siap tampil.
 * Memisahkan logika tampilan dari entity, use case, dan view sehingga
 * format output bisa diubah tanpa menyentuh domain.
 */
public class FinancePresenter {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Memformat nominal menjadi {@code Rp 1,000,000.00}: pemisah ribuan koma dan dua digit desimal.
     * Nilai negatif (mis. saldo defisit) ditulis {@code -Rp 25,000.00}.
     */
    public String formatMoney(long amount) {
        BigDecimal value = BigDecimal.valueOf(amount).setScale(2);
        String formatted = String.format(Locale.US, "%,.2f", value.abs());
        return (amount < 0 ? "-Rp " : "Rp ") + formatted;
    }

    /** Memformat satu transaksi: {@code [id] keterangan | Rp 1,000,000.00 | [Pemasukan]}. */
    public String format(Transaction transaction) {
        return String.format(
                "[%d] %s | %s | [%s]",
                transaction.getId(),
                transaction.getDescription(),
                formatMoney(transaction.getAmount()),
                transaction.getType().label()
        );
    }

    /** Memformat saldo: {@code Saldo: Rp 4,975,000.00}. */
    public String formatBalance(long balance) {
        return "Saldo: " + formatMoney(balance);
    }

    /** Memformat waktu pencatatan transaksi. */
    public String formatCreatedAt(Transaction transaction) {
        return transaction.getCreatedAt().format(DATE_TIME_FORMAT);
    }

    /** Pesan sukses setelah menambah transaksi. */
    public String showAddSuccess(Transaction transaction) {
        return "Berhasil mencatat transaksi: " + format(transaction);
    }

    /** Pesan sukses setelah menghapus transaksi. */
    public String showRemoveSuccess(Transaction transaction) {
        return "Berhasil menghapus transaksi: " + format(transaction);
    }

    /** Pesan gagal saat menghapus transaksi. */
    public String showRemoveFailed(int id) {
        return String.format("[!] Gagal menghapus transaksi dengan ID: %d.", id);
    }

    /** Pesan validasi umum dari domain. */
    public String showValidationError(String message) {
        return "[!] " + message;
    }
}
