package com.seuportfolio.cnab_processor.domain.service;

import com.seuportfolio.cnab_processor.domain.model.TransactionRecord;
import com.seuportfolio.cnab_processor.domain.model.enums.BankCode;

/**
 * Contrato para regras específicas de cada banco (Strategy Pattern).
 * Implementações entram na Fase 2.
 */
public interface BancoStrategy {

    /** Código do banco FEBRABAN que esta strategy suporta. */
    BankCode supportedBank();

    /**
     * Valida agência e conta do favorecido conforme as regras do banco.
     */
    boolean validateBankDetails(String agency, String account,
                                String agencyDv, String accountDv);

    /**
     * Aplica enriquecimentos ou normalizações específicas do banco
     * no registro já parseado.
     */
    void enrich(TransactionRecord record);
}