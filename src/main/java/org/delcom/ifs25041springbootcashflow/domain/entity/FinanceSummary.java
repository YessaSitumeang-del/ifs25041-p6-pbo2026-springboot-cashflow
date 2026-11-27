package org.delcom.ifs25041springbootcashflow.domain.entity;

/**
 * Ringkasan keuangan seorang pengguna: jumlah transaksi, total pemasukan, dan total pengeluaran.
 */
public record FinanceSummary(int count, long income, long expense) {

    /** Saldo = total pemasukan dikurangi total pengeluaran (bisa negatif). */
    public long balance() {
        return income - expense;
    }
}
