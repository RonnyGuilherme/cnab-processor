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
        // Null-safe: campos podem vir vazios do parser
        String rawAgency = record.getBeneficiaryAgency() != null
                ? record.getBeneficiaryAgency() : "";
        String normalizedAgency = String.format("%4s", rawAgency.replaceAll("\\D", ""))
                .replace(' ', '0');
        record.setBeneficiaryAgency(normalizedAgency);
        log.debug("Bradesco — agência normalizada: {}", normalizedAgency);

        String rawAccount = record.getBeneficiaryAccount() != null
                ? record.getBeneficiaryAccount() : "";
        String normalizedAccount = String.format("%07d",
                rawAccount.isBlank() ? 0L : Long.parseLong(rawAccount.replaceAll("\\D", "")));
        record.setBeneficiaryAccount(normalizedAccount);
        log.debug("Bradesco — conta normalizada: {}", normalizedAccount);
        // No final do enrich()
        if (!validateBankDetails(normalizedAgency, normalizedAccount, "", "")) {
            record.reject("Bradesco — DV inválido para agência/conta.");
        }
        }
    }