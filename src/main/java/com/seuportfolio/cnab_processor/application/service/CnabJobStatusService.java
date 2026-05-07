package com.seuportfolio.cnab_processor.application.service;

import com.seuportfolio.cnab_processor.application.batch.CnabJobOrchestrator;
import com.seuportfolio.cnab_processor.infrastructure.web.dto.JobStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Consulta o estado de execuções de job Spring Batch via {@link JobExplorer}.
 *
 * <p>O {@link JobExplorer} é read-only e não interfere no job em andamento,
 * tornando o polling leve e seguro para ser chamado com frequência.</p>
 */
@Service
@RequiredArgsConstructor
public class CnabJobStatusService {

    private final JobExplorer jobExplorer;

    public JobStatusResponse getStatus(Long jobExecutionId) {
        JobExecution execution = jobExplorer.getJobExecution(jobExecutionId);

        if (execution == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Job execution não encontrado: " + jobExecutionId);
        }

        String cnabFileId = execution.getExecutionContext()
                .getString(CnabJobOrchestrator.CTX_CNAB_FILE_ID, null);

        String failureMessage = execution.getAllFailureExceptions().stream()
                .map(Throwable::getMessage)
                .findFirst()
                .orElse(null);

        return new JobStatusResponse(
                execution.getId(),
                execution.getJobInstance().getJobName(),
                execution.getStatus().name(),
                execution.getExitStatus().getExitCode(),
                toLocalDateTime(execution.getStartTime()),
                toLocalDateTime(execution.getEndTime()),
                cnabFileId,
                failureMessage
        );
    }

    private LocalDateTime toLocalDateTime(LocalDateTime dateTime) {
        return dateTime;
    }
}