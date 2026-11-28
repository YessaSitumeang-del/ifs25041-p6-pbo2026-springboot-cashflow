package org.delcom.ifs25041springbootcashflow.domain.repository;

import org.delcom.ifs25041springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25041springbootcashflow.domain.exception.DomainException;

import java.util.List;

/**
 * Port layanan AI untuk menghasilkan transaksi dari catatan keuangan berbentuk teks bebas.
 */
public interface IFinanceAiService {

    /**
     * Menghasilkan daftar transaksi dari prompt teks bebas menggunakan model AI.
     *
     * @param prompt catatan keuangan dari pengguna
     * @return daftar transaksi hasil generasi
     * @throws DomainException jika terjadi kegagalan komunikasi AI atau parsing
     */
    List<AiGeneratedTransaction> generateTransactions(String prompt) throws DomainException;
}
