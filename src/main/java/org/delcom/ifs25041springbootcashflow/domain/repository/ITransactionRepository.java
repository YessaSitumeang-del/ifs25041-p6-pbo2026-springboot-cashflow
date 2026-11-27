package org.delcom.ifs25041springbootcashflow.domain.repository;

import org.delcom.ifs25041springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25041springbootcashflow.domain.entity.TransactionType;

import java.util.List;

/**
 * Port spesifik Transaction yang memperluas generic {@link IRepository}.
 * Menambahkan pembuatan transaksi baru dengan penomoran ID otomatis.
 */
public interface ITransactionRepository extends IRepository<Transaction, Integer> {
    /**
     * Menyimpan transaksi baru untuk pengguna tertentu.
     *
     * @param proofPath path relatif bukti gambar; boleh null
     */
    Transaction save(int userId, String description, long amount, TransactionType type, String proofPath);

    /** Mengambil semua transaksi milik satu pengguna. */
    List<Transaction> findByUserId(int userId);
}
