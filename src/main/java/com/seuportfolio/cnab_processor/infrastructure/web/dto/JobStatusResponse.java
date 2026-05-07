package com.seuportfolio.cnab_processor.infrastructure.web.dto;

import java.time.LocalDateTime;

/**
 * Resposta do endpoint de polling de status do job Spring Batch.
 */
public record JobStatusResponse(
        Long jobExecutionId,
        String jobName,
        String jobStatus,
        String exitCode,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String cnabFileId,
        String failureMessage
) {}