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
 *
 * <p>Particularidades:</p>
 * <ul>
 *   <li>Agência: 4 dígitos (sem DV)</li>
 *   <li>Conta:   5 dígitos + DV (Módulo 10)</li>
 *   <li>DV da conta é validado sobre a concatenação agência + conta (base 9 dígitos) + DV</li>
 * </ul>
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
            log.debug("Itaú — DV da conta inválido: agência={}, conta={}, dv={}",
                    agency, account, accountDv);
        }

        return valid;
    }

    @Override
    public void enrich(TransactionRecord record) {
        // ── null-safe e normalização da agência ──
        String rawAgency = record.getBeneficiaryAgency() != null
                ? record.getBeneficiaryAgency() : "";
        String normalizedAgency = String.format("%4s", rawAgency.replaceAll("\\D", ""))
                .replace(' ', '0');
        record.setBeneficiaryAgency(normalizedAgency);
        log.debug("Itaú — agência normalizada: {}", normalizedAgency);

        // ── null-safe e normalização da conta ──
        String rawAccount = record.getBeneficiaryAccount() != null
                ? record.getBeneficiaryAccount() : "";
        // Garantir ao menos 6 dígitos (5 conta + 1 DV), completando com zeros à esquerda se menor
        String accountDigits = rawAccount.replaceAll("\\D", "");
        String normalizedAccount = String.format("%06d",
                accountDigits.isBlank() ? 0L : Long.parseLong(accountDigits));
        record.setBeneficiaryAccount(normalizedAccount);
        log.debug("Itaú — conta normalizada: {}", normalizedAccount);

        // ── Normalização do documento (CPF/CNPJ) ──
        String rawDoc = record.getDocumentNumber() != null
                ? record.getDocumentNumber() : "";
        String normalizedDoc = rawDoc.replaceAll("\\D", "");
        record.setDocumentNumber(normalizedDoc);
        log.debug("Itaú — documento normalizado: {}", normalizedDoc);

        // ── Validação do DV da conta ──
        if (normalizedAccount.length() >= 6) {
            String agencyBase = normalizedAgency;                     // 4 dígitos
            String accountBase = normalizedAccount.substring(0, 5);  // 5 dígitos da conta
            String dv = normalizedAccount.substring(5);              // dígito verificador

            if (!validateBankDetails(agencyBase, accountBase, "", dv)) {
                record.reject("Itaú — dígito verificador da conta inválido.");
            }
        } else {
            record.reject("Itaú — conta não possui comprimento mínimo para validação.");
        }
    }
}