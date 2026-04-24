package com.seuportfolio.cnab_processor.application.service.banco;

import com.seuportfolio.cnab_processor.domain.model.TransactionRecord;
import com.seuportfolio.cnab_processor.domain.model.enums.BankCode;
import com.seuportfolio.cnab_processor.domain.service.BancoStrategy;
import com.seuportfolio.cnab_processor.domain.validator.Modulo10Validator;
import com.seuportfolio.cnab_processor.domain.validator.Modulo11Validator;
import com.seuportfolio.cnab_processor.domain.validator.Modulo11Validator.Variant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Regras específicas do Bradesco (código 237).
 * Particularidades:
 * - Agência: 4 dígitos + DV pelo Módulo 11 FEBRABAN (resto < 2 → DV = 0)
 * - Conta:   7 dígitos + DV pelo Módulo 10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BradescoStrategy implements BancoStrategy {

    private final Modulo10Validator modulo10Validator;
    private final Modulo11Validator modulo11Validator;

    @Override
    public BankCode supportedBank() {
        return BankCode.BRADESCO;
    }

    @Override
    public boolean validateBankDetails(String agency, String account,
                                       String agencyDv, String accountDv) {
        boolean agencyValid  = modulo11Validator.validate(agency + agencyDv, Variant.FEBRABAN);
        boolean accountValid = modulo10Validator.validate(account + accountDv);

        if (!agencyValid) {
            log.debug("Bradesco — DV de agência inválido: agência={}, dv={}", agency, agencyDv);
        }
        if (!accountValid) {
            log.debug("Bradesco — DV de conta inválido: conta={}, dv={}", account, accountDv);
        }

        return agencyValid && accountValid;
    }

    @Override
    public void enrich(TransactionRecord record) {
        // Bradesco: conta sempre com 7 dígitos, padding à esquerda com zeros
        String padded = String.format("%07d", Long.parseLong(
                record.getBeneficiaryAccount().replaceAll("\\D", "")
        ));
        log.debug("Bradesco — conta normalizada: {} → {}", record.getBeneficiaryAccount(), padded);
    }
}