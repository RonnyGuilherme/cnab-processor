package com.seuportfolio.cnab_processor.application.batch.reader;

import com.seuportfolio.cnab_processor.application.batch.CnabJobOrchestrator;
import com.seuportfolio.cnab_processor.application.service.CnabParserFactory;
import com.seuportfolio.cnab_processor.domain.model.CnabFile;
import com.seuportfolio.cnab_processor.domain.model.TransactionRecord;
import com.seuportfolio.cnab_processor.domain.model.enums.CnabType;
import com.seuportfolio.cnab_processor.domain.service.CnabParser;
import com.seuportfolio.cnab_processor.infrastructure.persistence.CnabFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.core.configuration.annotation.StepScope;      // ← import correto
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lê e parseia um arquivo CNAB, retornando um {@link TransactionRecord} por chamada.
 *
 * <p><b>Por que {@code @StepScope}?</b><br>
 * Este reader mantém estado ({@code iterator}, {@code persistedCnabFile}).
 * Como singleton, esse estado seria compartilhado entre execuções concorrentes,
 * causando corrupção silenciosa. {@code @StepScope} garante uma instância
 * exclusiva por execução do step.</p>
 *
 * <p><b>Por que apenas {@code @BeforeStep}/@{@code AfterStep} sem interface?</b><br>
 * Implementar {@code StepExecutionListener} E usar as anotações nos mesmos métodos
 * faz o Spring Batch invocá-los duas vezes. Como registramos o bean via
 * {@code .listener()}, as anotações sozinhas são suficientes.</p>
 *
 * <p><b>Nota de evolução (sugestão 4):</b><br>
 * Este reader carrega o arquivo inteiro em memória antes de iterar.
 * Para a Fase 3 (arquivos de remessa típicos: até ~50MB) é aceitável.
 * Se no futuro surgir necessidade de processar arquivos com milhões de linhas,
 * substituir por {@code FlatFileItemReader} com {@code LineMapper} personalizado
 * permitirá leitura preguiçosa linha a linha.</p>
 */
@Slf4j
@Component
@StepScope   // ← nova instância por execução de step — obrigatório para readers com estado
@RequiredArgsConstructor
public class CnabFileItemReader implements ItemReader<TransactionRecord> {

    // Sem "implements StepExecutionListener" — anotações @BeforeStep/@AfterStep
    // são suficientes quando o bean é registrado via .listener() no BatchConfiguration.
    // Implementar a interface junto com as anotações causaria dupla invocação.

    private final CnabParserFactory  parserFactory;
    private final CnabFileRepository cnabFileRepository;

    private Iterator<TransactionRecord> iterator;
    private CnabFile persistedCnabFile;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        String filePath = stepExecution.getJobParameters().getString(CnabJobOrchestrator.PARAM_FILE_PATH);
        String fileName = stepExecution.getJobParameters().getString(CnabJobOrchestrator.PARAM_FILE_NAME);

        log.info("CnabFileItemReader.beforeStep — arquivo: '{}'", fileName);

        try {
            List<String> lines = normalizar(Files.readAllLines(Path.of(filePath), StandardCharsets.UTF_8));

            if (lines.isEmpty()) {
                throw new IllegalArgumentException("Arquivo vazio ou sem linhas válidas: " + fileName);
            }

            CnabType   type   = CnabType.detect(lines.getFirst().length());
            CnabParser parser = parserFactory.getParser(type);

            CnabFile transientFile = parser.parse(lines, fileName);

            this.persistedCnabFile = cnabFileRepository.save(
                    CnabFile.receive(fileName, type, transientFile.getBankCode())
            );

            List<TransactionRecord> records = transientFile.getTransactions().stream()
                    .peek(record -> record.associateTo(this.persistedCnabFile))
                    .collect(Collectors.toList());

            this.iterator = records.iterator();

            stepExecution.getJobExecution()
                    .getExecutionContext()
                    .putString(CnabJobOrchestrator.CTX_CNAB_FILE_ID, persistedCnabFile.getId().toString());

            log.info("Reader pronto — banco: {}, tipo: {}, registros: {}",
                    persistedCnabFile.getBankCode().getCode(), type, records.size());

        } catch (Exception e) {
            log.error("Falha ao inicializar CnabFileItemReader: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao preparar leitura do arquivo CNAB: " + e.getMessage(), e);
        }
    }

    @Override
    public TransactionRecord read() {
        return (iterator != null && iterator.hasNext()) ? iterator.next() : null;
    }

    @AfterStep
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (persistedCnabFile != null) {
            long processed = stepExecution.getWriteCount();
            long rejected  = stepExecution.getSkipCount() + stepExecution.getReadSkipCount();

            persistedCnabFile.registerProcessingResult((int) processed, (int) rejected);
            cnabFileRepository.save(persistedCnabFile);

            log.info("CnabFileItemReader.afterStep — processados: {}, rejeitados: {}",
                    processed, rejected);
        }
        return ExitStatus.COMPLETED;
    }

    /**
     * Correção 8 — Remove BOM UTF-8, linhas em branco e espaços à direita.
     *
     * <p>Arquivos CNAB gerados por sistemas Windows frequentemente incluem
     * BOM (byte order mark) no início ou linhas em branco entre registros,
     * causando falha no {@link CnabType#detect(int)}.</p>
     */
    private List<String> normalizar(List<String> lines) {
        return lines.stream()
                .map(l -> l.replace("\uFEFF", ""))   // remove BOM UTF-8
                .map(l -> l.replace("\r", ""))        // remove \r do CRLF — preserva espaços significativos
                .filter(l -> !l.isBlank())            // remove linhas totalmente em branco
                .collect(Collectors.toList());
    }
}