package org.delcom.ifs25041springbootcashflow.domain.entity;

import java.util.Locale;
import java.util.Optional;

/**
 * Jenis transaksi keuangan: uang masuk atau uang keluar.
 */
public enum TransactionType {
    INCOME,
    EXPENSE;

    /** Label singkat untuk ditampilkan di UI. */
    public String label() {
        return switch (this) {
            case INCOME -> "Pemasukan";
            case EXPENSE -> "Pengeluaran";
        };
    }

    /**
     * Menerjemahkan teks bebas (mis. keluaran AI) menjadi tipe transaksi.
     * Menerima nama enum maupun label Indonesia, tanpa memedulikan huruf besar/kecil.
     */
    public static Optional<TransactionType> parse(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String normalized = text.trim().toUpperCase(Locale.ROOT);
        for (TransactionType type : values()) {
            if (type.name().equals(normalized) || type.label().toUpperCase(Locale.ROOT).equals(normalized)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
