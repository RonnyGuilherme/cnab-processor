package com.seuportfolio.cnab_processor.application.service;

import com.seuportfolio.cnab_processor.application.batch.CnabJobOrchestrator;
import com.seuportfolio.cnab_processor.domain.model.CnabFile;
import com.seuportfolio.cnab_processor.infrastructure.persistence.CnabFileRepository;
import com.seuportfolio.cnab_processor.infrastructure.web.dto.UploadResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço de aplicação responsável por receber um arquivo CNAB via upload,
 * persistir no diretório temporário e disparar o job Spring Batch de forma
 * <b>assíncrona</b>.
 *
 * <p>A idempotência é garantida através do hash SHA‑256 do conteúdo do arquivo.
 * Caso o arquivo já tenha sido processado, a resposta é retornada sem
 * reprocessamento.</p>
 *
 * <p>O job é lançado via {@code asyncJobLauncher} e retorna imediatamente com
 * o {@code jobExecutionId}. O cliente deve fazer polling de status via
 * {@code GET /api/v1/jobs/{jobExecutionId}}.</p>
 */
@Slf4j
@Service
public class CnabProcessingService {

    private final JobLauncher asyncJobLauncher;
    private final Job cnabProcessingJob;
    private final CnabFileRepository cnabFileRepository;
    private final MeterRegistry meterRegistry;

    @Value("${cnab.upload.temp-dir:${java.io.tmpdir}/cnab-processor}")
    private String tempDir;

    public CnabProcessingService(
            @Qualifier("asyncJobLauncher") JobLauncher asyncJobLauncher,
            Job cnabProcessingJob,
            CnabFileRepository cnabFileRepository,
            MeterRegistry meterRegistry) {
        this.asyncJobLauncher = asyncJobLauncher;
        this.cnabProcessingJob = cnabProcessingJob;
        this.cnabFileRepository = cnabFileRepository;
        this.meterRegistry = meterRegistry;
    }

    public UploadResponse process(MultipartFile file) {
        String originalName = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : "upload_" + UUID.randomUUID() + ".rem";

        log.info("Iniciando processamento assíncrono: '{}'", originalName);
        Timer.Sample timerSample = Timer.start(meterRegistry);

        try {
            String fileHash = computeHash(file);

            // Idempotência
            Optional<CnabFile> existing = cnabFileRepository.findByFileHash(fileHash);
            if (existing.isPresent()) {
                CnabFile f = existing.get();
                log.info("Arquivo duplicado (hash: {}). Retornando existente.", fileHash);
                return new UploadResponse(
                        f.getId(),
                        f.getOriginalFileName(),
                        "ALREADY_PROCESSED",
                        null,                      // jobExecutionId não se aplica
                        f.getProcessedLines(),
                        f.getRejectedLines(),
                        "Arquivo já processado. ID: " + f.getId()
                );
            }

            Path savedPath = saveToTempDir(file, originalName);

            JobParameters params = new JobParametersBuilder()
                    .addString(CnabJobOrchestrator.PARAM_FILE_PATH, savedPath.toString())
                    .addString(CnabJobOrchestrator.PARAM_FILE_NAME, originalName)
                    .addString("fileHash", fileHash)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            // Lança de forma assíncrona — retorna imediatamente
            JobExecution execution = asyncJobLauncher.run(cnabProcessingJob, params);

            log.info("Job '{}' lançado de forma assíncrona. Polling: GET /api/v1/jobs/{}",
                    execution.getId(), execution.getId());

            meterRegistry.counter("cnab.files.uploaded",
                    "status", execution.getStatus().name()).increment();

            return new UploadResponse(
                    null,                          // fileId ainda não disponível
                    originalName,
                    execution.getStatus().name(),
                    execution.getId(),             // jobExecutionId para polling
                    0, 0,
                    "Processamento iniciado. Consulte GET /api/v1/jobs/" + execution.getId()
            );

        } catch (Exception e) {
            log.error("Erro ao iniciar processamento: {}", e.getMessage(), e);
            meterRegistry.counter("cnab.files.uploaded", "status", "ERROR").increment();
            throw new RuntimeException("Falha ao processar arquivo CNAB: " + e.getMessage(), e);
        } finally {
            timerSample.stop(meterRegistry.timer("cnab.processing.duration",
                    "file", originalName));
        }
    }

    private String computeHash(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = file.getBytes();
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao computar hash do arquivo", e);
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
}