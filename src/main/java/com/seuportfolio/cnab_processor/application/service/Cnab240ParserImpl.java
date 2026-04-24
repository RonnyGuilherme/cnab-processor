package com.seuportfolio.cnab_processor.application.service;

import com.seuportfolio.cnab_processor.domain.model.CnabFile;
import com.seuportfolio.cnab_processor.domain.model.TransactionRecord;
import com.seuportfolio.cnab_processor.domain.model.enums.BankCode;
import com.seuportfolio.cnab_processor.domain.model.enums.CnabType;
import com.seuportfolio.cnab_processor.domain.service.CnabParser;
import com.seuportfolio.cnab_processor.infrastructure.exception.CnabParsingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.seuportfolio.cnab_processor.application.service.FixedLengthExtractor.*;

/**
 * Parser CNAB 240 — Manual FEBRABAN v10.7.
 *
 * Tipos de registro:
 *   0 → Header de arquivo
 *   1 → Header de lote
 *   3 → Detalhe (Segmento A, B, J...)
 *   5 → Trailer de lote
 *   9 → Trailer de arquivo
 */
@Slf4j
@Component
public class Cnab240ParserImpl implements CnabParser {

    private static final int    LINE_LENGTH = 240;
    private static final String DETAIL_TYPE = "3";
    private static final String SEGMENT_A   = "A";

    @Override
    public CnabType supportedType() {
        return CnabType.CNAB240;
    }

    @Override
    public CnabFile parse(List<String> lines, String originalFileName) {
        log.info("Iniciando parse CNAB240 — arquivo: '{}', total de linhas: {}",
                originalFileName, lines.size());

        if (lines == null || lines.isEmpty()) {
            throw new CnabParsingException(
                    "Arquivo CNAB240 vazio ou nulo: '%s'".formatted(originalFileName)
            );
        }

        String firstLine = lines.getFirst();
        assertLineLength(firstLine, 1, originalFileName);

        BankCode bankCode = BankCode.fromCode(extract(firstLine, 1, 3));
        CnabFile cnabFile = CnabFile.receive(originalFileName, CnabType.CNAB240, bankCode);

        int processed = 0;
        int rejected  = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            int    lineNumber = i + 1;
            String line       = lines.get(i);

            if (line.length() != LINE_LENGTH) {
                String msg = "Linha %d com tamanho inválido: %d (esperado %d)."
                        .formatted(lineNumber, line.length(), LINE_LENGTH);
                log.warn(msg);
                errors.add(msg);
                rejected++;
                continue;
            }

            if (DETAIL_TYPE.equals(extract(line, 8, 8))
                    && SEGMENT_A.equals(extract(line, 14, 14))) {
                try {
                    cnabFile.addTransaction(parseSegmentA(line, lineNumber));
                    processed++;
                } catch (Exception e) {
                    log.warn("Falha ao parsear Segmento A — linha {}: {}", lineNumber, e.getMessage());
                    errors.add("Linha %d: %s".formatted(lineNumber, e.getMessage()));
                    rejected++;
                }
            }
        }

        cnabFile.registerProcessingResult(processed, rejected);

        log.info("Parse CNAB240 concluído — banco: {}, processados: {}, rejeitados: {}",
                bankCode.getCode(), processed, rejected);

        if (!errors.isEmpty()) {
            log.debug("Erros de parse: {}", errors);
        }

        return cnabFile;
    }

    private TransactionRecord parseSegmentA(String line, int lineNumber) {
        return TransactionRecord.builder()
                .lineNumber(lineNumber)
                .beneficiaryAgency(extract(line, 21, 25))
                .beneficiaryAccount(extract(line, 27, 38))
                .beneficiaryName(extract(line, 44, 73))
                .documentNumber(extract(line, 74, 93))
                .paymentDate(extractDate(line, 94, 101))
                .currencyType(extract(line, 102, 104))
                .amount(extractAmount(line, 120, 134))
                .payerDocument("")   // Segmento B — Fase 2
                .rawLine(line)
                .build();
    }

    private void assertLineLength(String line, int lineNumber, String fileName) {
        if (line.length() != LINE_LENGTH) {
            throw new CnabParsingException(
                    "Header do arquivo '%s' (linha %d) tem tamanho inválido: %d (esperado %d)."
                            .formatted(fileName, lineNumber, line.length(), LINE_LENGTH)
            );
        }
    }
}