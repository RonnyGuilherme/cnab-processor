package com.seuportfolio.cnab_processor.infrastructure.web.dto;

import java.util.UUID;

public record UploadResponse(
        UUID fileId,
        String fileName,
        String jobStatus,
        int processedRecords,
        int rejectedRecords,
        String message
) {}