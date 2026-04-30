package com.seuportfolio.cnab_processor.infrastructure.web.dto;

import com.seuportfolio.cnab_processor.domain.model.CnabFile;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resposta da API para um arquivo CNAB processado.
 */
public record CnabFileResponse(
        UUID id,
        String fileName,
        String bankCode,
        String cnabType,
        String status,
        int totalRecords,
        int processedRecords,
        int rejectedRecords,
        LocalDateTime receivedAt
) {
    public static CnabFileResponse from(CnabFile file) {
        return new CnabFileResponse(
                file.getId(),
                file.getOriginalFileName(),
                file.getBankCode() != null ? file.getBankCode().getCode() : "N/A",
                file.getCnabType() != null ? file.getCnabType().name() : "N/A",
                file.getStatus() != null ? file.getStatus().name() : "N/A",
                file.getTotalLines(),
                file.getProcessedLines(),
                file.getRejectedLines(),
                file.getReceivedAt()
        );
    }
}