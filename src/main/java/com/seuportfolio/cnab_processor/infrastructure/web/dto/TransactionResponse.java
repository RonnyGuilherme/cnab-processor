package com.seuportfolio.cnab_processor.infrastructure.web.dto;

import com.seuportfolio.cnab_processor.domain.model.TransactionRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID cnabFileId,
        String payerDocument,
        String beneficiaryName,
        String beneficiaryAgency,
        String beneficiaryAccount,
        BigDecimal amount,
        LocalDate paymentDate,
        String currencyType,
        String documentNumber,
        String status,
        String rejectionReason,
        int lineNumber
) {
    public static TransactionResponse from(TransactionRecord t) {
        return new TransactionResponse(
                t.getId(),
                t.getCnabFile() != null ? t.getCnabFile().getId() : null,
                t.getPayerDocument(),
                t.getBeneficiaryName(),
                t.getBeneficiaryAgency(),
                t.getBeneficiaryAccount(),
                t.getAmount(),
                t.getPaymentDate(),
                t.getCurrencyType(),
                t.getDocumentNumber(),
                t.getStatus() != null ? t.getStatus().name() : null,
                t.getRejectionReason(),
                t.getLineNumber()
        );
    }
}