package org.delcom.ifs25041springbootcashflow.usecase;

import org.delcom.ifs25041springbootcashflow.domain.entity.FinanceSummary;
import org.delcom.ifs25041springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25041springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25041springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25041springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25041springbootcashflow.domain.entity.User;
import org.delcom.ifs25041springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25041springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25041springbootcashflow.domain.repository.ITransactionRepository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Use case yang menangani logika bisnis pencatatan transaksi keuangan.
 * Tidak melakukan I/O selain lewat port repository; semua aturan validasi dan
 * kepemilikan data dipusatkan di sini agar konsisten dan mudah diuji.
 */
public class FinanceUseCase {
    /** Port repository yang di-inject dari luar (Dependency Inversion). */
    private final ITransactionRepository transactionRepository;

    public FinanceUseCase(ITransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /** Mengambil semua transaksi milik satu pengguna, terbaru di atas. */
    public List<Transaction> getTransactionsByUser(int userId) {
        return listTransactions(userId, null, TransactionTypeFilter.ALL, SortOption.NEWEST_FIRST);
    }

    /**
     * Mengambil transaksi pengguna dengan pencarian keterangan, filter tipe, dan urutan.
     * Keyword kosong/null diabaikan. Filter/sort null memakai default (semua tipe + terbaru).
     */
    public List<Transaction> listTransactions(
            int userId,
            String keyword,
            TransactionTypeFilter typeFilter,
            SortOption sortOption
    ) {
        TransactionTypeFilter filter = typeFilter == null ? TransactionTypeFilter.ALL : typeFilter;
        SortOption sort = sortOption == null ? SortOption.NEWEST_FIRST : sortOption;

        Stream<Transaction> stream = transactionRepository.findByUserId(userId).stream()
                .filter(filter::matches);

        if (keyword != null && !keyword.isBlank()) {
            String lowerKeyword = keyword.trim().toLowerCase(Locale.ROOT);
            stream = stream.filter(transaction ->
                    transaction.getDescription().toLowerCase(Locale.ROOT).contains(lowerKeyword));
        }

        return stream.sorted(sort.comparator()).toList();
    }

    /**
     * Mengambil transaksi milik {@code ownerId} untuk dilihat oleh {@code viewer} (read-only).
     * Pemilik boleh melihat miliknya sendiri; ADMIN boleh melihat milik siapa pun.
     */
    public List<Transaction> getTransactionsOf(User viewer, int ownerId) throws ValidationException {
        if (!viewer.isAdmin() && viewer.getId() != ownerId) {
            throw new ValidationException("Anda tidak berhak melihat transaksi pengguna ini");
        }
        return getTransactionsByUser(ownerId);
    }

    /** Menghitung ringkasan keuangan seorang pengguna dengan satu kali baca data. */
    public FinanceSummary getSummary(int userId) {
        long income = 0;
        long expense = 0;
        List<Transaction> transactions = transactionRepository.findByUserId(userId);
        for (Transaction transaction : transactions) {
            if (transaction.getType() == TransactionType.INCOME) {
                income += transaction.getAmount();
            } else {
                expense += transaction.getAmount();
            }
        }
        return new FinanceSummary(transactions.size(), income, expense);
    }

    /** Saldo pengguna: total pemasukan dikurangi total pengeluaran. */
    public long getBalance(int userId) {
        return getSummary(userId).balance();
    }

    /**
     * Memvalidasi input form (keterangan dan jumlah mentah dari kolom teks) dan
     * mengembalikan jumlah yang sudah dikonversi. Dipanggil sebelum efek samping
     * lain (mis. menyimpan file bukti) agar tidak meninggalkan sisa data bila input salah.
     */
    public long validateInput(String description, String rawAmount) throws ValidationException {
        validateDescription(description);
        return parseAmount(rawAmount);
    }

    /** Menambahkan transaksi baru setelah validasi. {@code proofPath} boleh null/kosong. */
    public Transaction addTransaction(
            int userId,
            String description,
            long amount,
            TransactionType type,
            String proofPath
    ) throws ValidationException {
        validateDescription(description);
        if (amount <= 0) {
            throw new ValidationException("Jumlah harus lebih dari 0");
        }
        if (type == null) {
            throw new ValidationException("Tipe transaksi tidak boleh kosong");
        }
        String safeProof = (proofPath == null || proofPath.isBlank()) ? null : proofPath;
        return transactionRepository.save(userId, description.trim(), amount, type, safeProof);
    }

    /**
     * Menghapus transaksi milik pengguna tertentu. Mengembalikan transaksi yang dihapus
     * agar pemanggil dapat membersihkan bukti gambarnya.
     */
    public Transaction removeTransactionForUser(int userId, int id)
            throws EntityNotFoundException, ValidationException {
        if (id <= 0) {
            throw new ValidationException("ID transaksi tidak valid");
        }
        Optional<Transaction> found = transactionRepository.findById(id);
        if (found.isEmpty()) {
            throw new EntityNotFoundException("Transaksi", id);
        }
        Transaction transaction = found.get();
        if (transaction.getUserId() != userId) {
            throw new ValidationException("Transaksi tidak milik pengguna ini");
        }
        if (!transactionRepository.deleteById(id)) {
            throw new EntityNotFoundException("Transaksi", id);
        }
        return transaction;
    }

    private void validateDescription(String description) throws ValidationException {
        if (description == null || description.trim().isEmpty()) {
            throw new ValidationException("Keterangan tidak boleh kosong");
        }
    }

    private long parseAmount(String rawAmount) throws ValidationException {
        if (rawAmount == null || rawAmount.isBlank()) {
            throw new ValidationException("Jumlah tidak boleh kosong");
        }
        long amount;
        try {
            amount = Long.parseLong(rawAmount.trim());
        } catch (NumberFormatException ex) {
            throw new ValidationException("Jumlah harus berupa angka bulat (tanpa titik, koma, atau huruf)");
        }
        if (amount <= 0) {
            throw new ValidationException("Jumlah harus lebih dari 0");
        }
        return amount;
    }
}
