package com.seuportfolio.cnab_processor.application.service;

import com.seuportfolio.cnab_processor.application.batch.CnabJobOrchestrator;
import com.seuportfolio.cnab_processor.infrastructure.persistence.CnabFileRepository;
import com.seuportfolio.cnab_processor.infrastructure.web.dto.UploadResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Serviço de aplicação responsável por receber um arquivo CNAB via upload,
 * persistir no diretório temporário e disparar o job Spring Batch.
 *
 * <p><b>Decisão de design:</b> O job é executado de forma síncrona para simplificar
 * a resposta da API (o cliente recebe o resultado imediatamente). Em produção
 * com arquivos grandes, recomenda-se tornar o processamento assíncrono e oferecer
 * um endpoint de polling por status via {@code jobExecutionId}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CnabProcessingService {

    private final JobLauncher jobLauncher;
    private final Job cnabProcessingJob;
    private final CnabFileRepository cnabFileRepository;
    private final MeterRegistry meterRegistry;

    @Value("${cnab.upload.temp-dir:${java.io.tmpdir}/cnab-processor}")
    private String tempDir;

    public UploadResponse process(MultipartFile file) {
        String originalName = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : "upload_" + UUID.randomUUID() + ".rem";

        log.info("Iniciando processamento do arquivo: '{}'", originalName);
        Timer.Sample timerSample = Timer.start(meterRegistry);

        try {
            Path savedPath = saveToTempDir(file, originalName);

            JobParameters params = new JobParametersBuilder()
                    .addString(CnabJobOrchestrator.PARAM_FILE_PATH, savedPath.toString())
                    .addString(CnabJobOrchestrator.PARAM_FILE_NAME, originalName)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(cnabProcessingJob, params);

            log.info("Job '{}' finalizado com status: {}", execution.getJobId(), execution.getStatus());

            // Métricas
            meterRegistry.counter("cnab.files.uploaded",
                    "status", execution.getStatus().name()).increment();

            String cnabFileId = execution.getExecutionContext()
                    .getString(CnabJobOrchestrator.CTX_CNAB_FILE_ID, null);

            if (cnabFileId != null) {
                return cnabFileRepository.findById(java.util.UUID.fromString(cnabFileId))
                        .map(f -> {
                            meterRegistry.counter("cnab.transactions.processed")
                                    .increment(f.getProcessedLines());
                            meterRegistry.counter("cnab.transactions.rejected")
                                    .increment(f.getRejectedLines());

                            return new UploadResponse(
                                    f.getId(),
                                    f.getOriginalFileName(),
                                    execution.getStatus().name(),
                                    f.getProcessedLines(),
                                    f.getRejectedLines(),
                                    "Arquivo processado com sucesso."
                            );
                        })
                        .orElse(buildErrorResponse(originalName, execution, "CnabFile não encontrado após job."));
            }

            return buildErrorResponse(originalName, execution, "Job concluído sem ID de arquivo no contexto.");

        } catch (Exception e) {
            log.error("Erro ao processar arquivo '{}': {}", originalName, e.getMessage(), e);
            meterRegistry.counter("cnab.files.uploaded", "status", "ERROR").increment();
            throw new RuntimeException("Falha ao processar o arquivo CNAB: " + e.getMessage(), e);
        } finally {
            timerSample.stop(meterRegistry.timer("cnab.processing.duration",
                    "file", originalName));
        }
    }

    private Path saveToTempDir(MultipartFile file, String fileName) throws IOException {
        Path dir = Paths.get(tempDir);
        Files.createDirectories(dir);
        Path dest = dir.resolve(System.currentTimeMillis() + "_" + fileName);
        file.transferTo(dest.toFile());
        log.debug("Arquivo salvo em: {}", dest);
        return dest;
    }

    private UploadResponse buildErrorResponse(String name, JobExecution exec, String msg) {
        return new UploadResponse(null, name, exec.getStatus().name(), 0, 0, msg);
    }
}