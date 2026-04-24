package com.seuportfolio.cnab_processor.application.service.banco;

import com.seuportfolio.cnab_processor.domain.model.TransactionRecord;
import com.seuportfolio.cnab_processor.domain.model.enums.BankCode;
import com.seuportfolio.cnab_processor.domain.service.BancoStrategy;
import com.seuportfolio.cnab_processor.domain.validator.Modulo11Validator;
import com.seuportfolio.cnab_processor.domain.validator.Modulo11Validator.Variant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Regras específicas do Banco do Brasil (código 001).
 * Particularidades:
 * - Agência: 4 dígitos + DV pelo Módulo 11 variante BB (resto < 2 → DV = 1)
 * - Conta:   8 dígitos + DV pelo Módulo 11 variante BB
 * - Convênio: 6 dígitos sem DV
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BancoDoBrasilStrategy implements BancoStrategy {

    private final Modulo11Validator modulo11Validator;

    @Override
    public BankCode supportedBank() {
        return BankCode.BB;
    }

    @Override
    public boolean validateBankDetails(String agency, String account,
                                       String agencyDv, String accountDv) {
        boolean agencyValid  = modulo11Validator.validate(agency + agencyDv, Variant.BANCO_DO_BRASIL);
        boolean accountValid = modulo11Validator.validate(account + accountDv, Variant.BANCO_DO_BRASIL);

        if (!agencyValid) {
            log.debug("BB — DV de agência inválido: agência={}, dv={}", agency, agencyDv);
        }
        if (!accountValid) {
            log.debug("BB — DV de conta inválido: conta={}, dv={}", account, accountDv);
        }

        return agencyValid && accountValid;
    }

    @Override
    public void enrich(TransactionRecord record) {
        // BB: normaliza agência para 4 dígitos com zeros à esquerda
        String normalized = record.getBeneficiaryAgency()
                .replaceAll("\\D", "")
                .formatted("%4s")
                .replace(' ', '0');

        log.debug("BB — agência normalizada: {} → {}", record.getBeneficiaryAgency(), normalized);
    }
}
