package org.delcom.ifs25041springbootcashflow.usecase;

import org.delcom.ifs25041springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25041springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25041springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25041springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25041springbootcashflow.domain.repository.IFinanceAiService;
import org.delcom.ifs25041springbootcashflow.domain.repository.ITransactionRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Use case yang menangani pembuatan transaksi dari catatan keuangan menggunakan AI.
 */
public class FinanceAiUseCase {

    private final IFinanceAiService aiService;
    private final ITransactionRepository transactionRepository;

    public FinanceAiUseCase(IFinanceAiService aiService, ITransactionRepository transactionRepository) {
        this.aiService = aiService;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Meminta AI menguraikan catatan keuangan menjadi daftar transaksi.
     *
     * @param prompt catatan keuangan dari pengguna
     * @return daftar transaksi hasil generasi (belum disimpan)
     * @throws DomainException jika validasi prompt gagal atau AI bermasalah
     */
    public List<AiGeneratedTransaction> generateTransactions(String prompt) throws DomainException {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new ValidationException("Catatan keuangan tidak boleh kosong");
        }
        return aiService.generateTransactions(prompt.trim());
    }

    /**
     * Menyimpan transaksi hasil generasi AI ke basis data untuk pengguna tertentu.
     *
     * @throws ValidationException jika daftar item kosong
     */
    public List<Transaction> saveGeneratedTransactions(int userId, List<AiGeneratedTransaction> items)
            throws ValidationException {
        if (items == null || items.isEmpty()) {
            throw new ValidationException("Daftar transaksi hasil generate tidak boleh kosong");
        }
        List<Transaction> saved = new ArrayList<>();
        for (AiGeneratedTransaction item : items) {
            saved.add(transactionRepository.save(
                    userId, item.description(), item.amount(), item.type(), null));
        }
        return saved;
    }
}
