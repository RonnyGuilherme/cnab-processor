package com.seuportfolio.cnab_processor.application.service.banco;

import com.seuportfolio.cnab_processor.domain.model.TransactionRecord;
import com.seuportfolio.cnab_processor.domain.model.enums.BankCode;
import com.seuportfolio.cnab_processor.domain.service.BancoStrategy;
import com.seuportfolio.cnab_processor.domain.validator.Modulo10Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Regras específicas do Itaú Unibanco (código 341).
 * Particularidades:
 * - Agência: 4 dígitos sem DV separado
 * - Conta:   5 dígitos + DV pelo Módulo 10
 * - DV da conta é validado sobre a concatenação agência + conta (9 dígitos)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ItauStrategy implements BancoStrategy {

    private final Modulo10Validator modulo10Validator;

    @Override
    public BankCode supportedBank() {
        return BankCode.ITAU;
    }

    @Override
    public boolean validateBankDetails(String agency, String account,
                                       String agencyDv, String accountDv) {
        // Itaú: DV calculado sobre agência (4) + conta (5) = 9 dígitos concatenados
        String base = agency + account;
        boolean valid = modulo10Validator.validate(base + accountDv);

        if (!valid) {
            log.debug("Itaú — DV inválido: agência={}, conta={}, dv={}", agency, account, accountDv);
        }

        return valid;
    }

    @Override
    public void enrich(TransactionRecord record) {
        // Itaú: remove formatação do número do documento (traços e pontos)
        String cleaned = record.getDocumentNumber().replaceAll("[^\\d]", "");
        log.debug("Itaú — documento normalizado: {} → {}", record.getDocumentNumber(), cleaned);
    }
}