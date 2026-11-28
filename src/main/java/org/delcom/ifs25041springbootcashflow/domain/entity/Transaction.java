package org.delcom.ifs25041springbootcashflow.domain.entity;

import java.time.LocalDateTime;

/**
 * Entity inti yang merepresentasikan satu transaksi keuangan milik seorang pengguna.
 * Berada di layer domain, bebas dari urusan tampilan maupun penyimpanan.
 * Transaksi bersifat tidak dapat diubah (immutable) setelah dibuat.
 */
public class Transaction {
    /** ID unik transaksi. */
    private final int id;

    /** Pemilik transaksi. */
    private final int userId;

    /** Keterangan transaksi. */
    private final String description;

    /** Nominal dalam rupiah utuh; selalu positif, arah ditentukan oleh {@link #type}. */
    private final long amount;

    /** Pemasukan atau pengeluaran. */
    private final TransactionType type;

    /** Path relatif bukti gambar (opsional). */
    private final String proofPath;

    /** Waktu transaksi dicatat. */
    private final LocalDateTime createdAt;

    public Transaction(
            int id,
            int userId,
            String description,
            long amount,
            TransactionType type,
            String proofPath,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.proofPath = proofPath;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getDescription() {
        return description;
    }

    public long getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public String getProofPath() {
        return proofPath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** True jika transaksi memiliki bukti gambar. */
    public boolean hasProof() {
        return proofPath != null && !proofPath.isBlank();
    }
}
