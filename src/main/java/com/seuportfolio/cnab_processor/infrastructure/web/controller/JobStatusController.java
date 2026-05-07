package com.seuportfolio.cnab_processor.infrastructure.web.controller;

import com.seuportfolio.cnab_processor.application.service.CnabJobStatusService;
import com.seuportfolio.cnab_processor.infrastructure.web.dto.JobStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint de polling para acompanhar o processamento assíncrono de um arquivo CNAB.
 *
 * <p><b>Fluxo de uso:</b></p>
 * <ol>
 *   <li>Cliente faz POST /upload → recebe {@code jobExecutionId} imediatamente (status STARTING)</li>
 *   <li>Cliente faz polling em GET /jobs/{id} até status ser COMPLETED ou FAILED</li>
 *   <li>Com o {@code cnabFileId} retornado, consulta transações via /files/{id}/transactions</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Polling de status de processamento assíncrono de arquivos CNAB")
public class JobStatusController {

    private final CnabJobStatusService jobStatusService;

    @GetMapping("/{jobExecutionId}")
    @Operation(
            summary = "Consulta o status de um job CNAB",
            description = """
            Retorna o estado atual de uma execução de job Spring Batch.
            Status possíveis: STARTING, STARTED, COMPLETED, FAILED, STOPPED, UNKNOWN.
            Quando COMPLETED, o campo cnabFileId permite consultar as transações processadas.
            """
    )
    public ResponseEntity<JobStatusResponse> getJobStatus(
            @Parameter(description = "ID da execução retornado pelo upload")
            @PathVariable Long jobExecutionId) {
        return ResponseEntity.ok(jobStatusService.getStatus(jobExecutionId));
    }
}