package com.seuportfolio.cnab_processor.application.batch.reader;

import com.seuportfolio.cnab_processor.application.batch.CnabJobOrchestrator;
import com.seuportfolio.cnab_processor.application.service.CnabParserFactory;
import com.seuportfolio.cnab_processor.domain.model.CnabFile;
import com.seuportfolio.cnab_processor.domain.model.ParseResult;
import com.seuportfolio.cnab_processor.domain.model.TransactionRecord;
import com.seuportfolio.cnab_processor.domain.model.enums.CnabType;
import com.seuportfolio.cnab_processor.domain.model.enums.TransactionStatus;
import com.seuportfolio.cnab_processor.domain.service.CnabParser;
import com.seuportfolio.cnab_processor.infrastructure.persistence.CnabFileRepository;
import com.seuportfolio.cnab_processor.infrastructure.persistence.TransactionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ItemReader CNAB com leitura em streaming (BufferedReader linha a linha).
 *
 * <p><b>Melhoria em relação à Fase 3:</b> substitui {@code Files.readAllLines()}
 * por {@code BufferedReader} com leitura incremental, eliminando o risco de
 * OutOfMemoryError em arquivos grandes.</p>
 *
 * <p><b>Lookahead de 1 registro:</b> o último registro (segmento A ou J) fica
 * em {@code pendingRecord} até que o próximo A/J ou o fim do arquivo seja
 * encontrado. Isso permite que o segmento B enriqueça o registro A anterior
 * antes de emiti-lo.</p>
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class CnabFileItemReader implements ItemReader<TransactionRecord> {

    @Value("#{jobParameters['filePath']}")
    private String filePath;

    @Value("#{jobParameters['fileName']}")
    private String fileName;

    @Value("#{jobParameters['fileHash']}")
    private String fileHash;

    private final CnabParserFactory parserFactory;
    private final CnabFileRepository cnabFileRepository;
    private final TransactionRecordRepository transactionRecordRepository;

    private BufferedReader bufferedReader;
    private CnabParser parser;
    private CnabFile persistedCnabFile;
    private int lineNumber = 0;
    private boolean eof = false;

    // ── Lookahead: registro pendente aguarda possível segmento B ─────────────
    private TransactionRecord pendingRecord = null;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        log.info("CnabFileItemReader.beforeStep — arquivo: '{}'", fileName);

        try {
            Path path = Path.of(filePath);
            bufferedReader = Files.newBufferedReader(path, StandardCharsets.UTF_8);

            // Lê e processa o BOM (Byte Order Mark), se presente
            bufferedReader.mark(4);
            int first = bufferedReader.read();
            if (first == 0xFEFF) {
                log.debug("BOM detectado e removido.");
            } else {
                bufferedReader.reset();
            }

            // Lê a primeira linha para detectar tipo e banco (header)
            String headerLine = bufferedReader.readLine();
            lineNumber = 1;

            if (headerLine == null) {
                throw new IllegalStateException("Arquivo vazio: " + filePath);
            }

            // Remove \r se presente
            headerLine = headerLine.replace("\r", "");

            // Detecta tipo CNAB pela largura da linha
            CnabType cnabType = CnabType.detect(headerLine.length());
            parser = parserFactory.getParser(cnabType);


            // Cria CnabFile a partir do header
            CnabFile cnabFile = parser.parseHeader(headerLine, fileName);
            cnabFile.setFileHash(fileHash);
            persistedCnabFile = cnabFileRepository.save(cnabFile);

            // Expõe o ID no contexto para CnabProcessingService
            stepExecution.getExecutionContext()
                    .putString(CnabJobOrchestrator.CTX_CNAB_FILE_ID,
                            persistedCnabFile.getId().toString());

            log.info("Reader pronto — banco: {}, tipo: {}, arquivo: {}",
                    cnabFile.getBankCode(), cnabType, fileName);

        } catch (IOException e) {
            throw new RuntimeException("Falha ao abrir arquivo CNAB: " + filePath, e);
        }
    }

    /**
     * Retorna a próxima transação do arquivo ou {@code null} quando encerrado.
     *
     * <p>O algoritmo de lookahead garante que segmento B sempre enriquece
     * o segmento A anterior antes de emiti-lo.</p>
     */
    @Override
    public TransactionRecord read() throws Exception {
        // Se já chegou ao fim, drena o último pending
        if (eof) {
            return drainPending();
        }

        while (true) {
            String rawLine = bufferedReader.readLine();

            if (rawLine == null) {
                // Fim do arquivo — emite flush do parser e pending
                eof = true;
                ParseResult flushResult = parser.flush();
                if (flushResult.record() != null) {
                    // Segmento pendente no parser
                    TransactionRecord flushed = flushResult.record();
                    flushed.associateTo(persistedCnabFile);
                    return flushed;
                }
                return drainPending();
            }

            lineNumber++;
            String line = rawLine.replace("\r", "");

            ParseResult result = parser.parseLine(line, lineNumber, persistedCnabFile);

            switch (result.type()) {
                case SEGMENT_A, SEGMENT_J -> {
                    // Resultado é o registro anterior (pendente)
                    TransactionRecord previous = pendingRecord;
                    // Novo pending
                    pendingRecord = result.record();

                    if (previous != null) {
                        return previous;
                    }
                    // Sem pending anterior: continua lendo
                }
                case SEGMENT_B -> {
                    // Enriquecimento já aplicado diretamente no pendingRecord pelo parser
                    // Nada a emitir
                }
                case SKIP -> {
                    // Linha ignorada: continua
                }
            }
        }
    }

    @AfterStep
    public ExitStatus afterStep(StepExecution stepExecution) {
        // Fecha o reader
        if (bufferedReader != null) {
            try { bufferedReader.close(); } catch (IOException ignored) {}
        }

        if (persistedCnabFile != null) {
            long written  = stepExecution.getWriteCount();
            long rejected = transactionRecordRepository.countByCnabFileIdAndStatus(
                    persistedCnabFile.getId(), TransactionStatus.REJECTED);
            long processed = written - rejected;

            persistedCnabFile.registerProcessingResult((int) processed, (int) rejected);
            cnabFileRepository.save(persistedCnabFile);

            log.info("afterStep — escritos: {}, processados: {}, rejeitados: {}",
                    written, processed, rejected);
        }

        return ExitStatus.COMPLETED;
    }

    private TransactionRecord drainPending() {
        if (pendingRecord != null) {
            TransactionRecord result = pendingRecord;
            pendingRecord = null;
            return result;
        }
        return null;
    }
}