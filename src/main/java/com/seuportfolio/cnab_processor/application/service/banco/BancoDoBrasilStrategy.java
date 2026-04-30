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
 *
 * <p>Particularidades:</p>
 * <ul>
 *   <li>Agência: 4 dígitos + DV (Módulo 11 FEBRABAN)</li>
 *   <li>Conta:   12 dígitos + DV (Módulo 11 FEBRABAN)</li>
 * </ul>
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
        // ── null-safe e normalização da agência ──
        String rawAgency = record.getBeneficiaryAgency() != null
                ? record.getBeneficiaryAgency() : "";
        String normalizedAgency = String.format("%4s", rawAgency.replaceAll("\\D", ""))
                .replace(' ', '0');
        record.setBeneficiaryAgency(normalizedAgency);
        log.debug("BB — agência normalizada: {}", normalizedAgency);

        // ── null-safe e normalização da conta ──
        String rawAccount = record.getBeneficiaryAccount() != null
                ? record.getBeneficiaryAccount() : "";
        String normalizedAccount = String.format("%12s", rawAccount.replaceAll("\\D", ""))
                .replace(' ', '0');
        record.setBeneficiaryAccount(normalizedAccount);
        log.debug("BB — conta normalizada: {}", normalizedAccount);

        // ── Validação de DV ──
        if (!validateBankDetails(normalizedAgency, normalizedAccount, "", "")) {
            record.reject("BB — dígito verificador inválido para agência/conta.");
        }
    }
}