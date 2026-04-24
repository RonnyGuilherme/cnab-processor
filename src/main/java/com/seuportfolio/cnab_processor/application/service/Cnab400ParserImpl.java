package com.seuportfolio.cnab_processor.application.service;

import com.seuportfolio.cnab_processor.domain.model.CnabFile;
import com.seuportfolio.cnab_processor.domain.model.TransactionRecord;
import com.seuportfolio.cnab_processor.domain.model.enums.BankCode;
import com.seuportfolio.cnab_processor.domain.model.enums.CnabType;
import com.seuportfolio.cnab_processor.domain.service.CnabParser;
import com.seuportfolio.cnab_processor.infrastructure.exception.CnabParsingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.seuportfolio.cnab_processor.application.service.FixedLengthExtractor.*;

/**
 * Parser CNAB 400 — Manual FEBRABAN v018.
 *
 * Tipos de registro:
 *   0 → Header de arquivo
 *   1 → Detalhe (transação)
 *   9 → Trailer de arquivo
 */
@Slf4j
@Component
public class Cnab400ParserImpl implements CnabParser {

    private static final int    LINE_LENGTH   = 400;
    private static final String DETAIL_RECORD = "1";

    @Override
    public CnabType supportedType() {
        return CnabType.CNAB400;
    }

    @Override
    public CnabFile parse(List<String> lines, String originalFileName) {
        log.info("Iniciando parse CNAB400 — arquivo: '{}', total de linhas: {}",
                originalFileName, lines.size());

        if (lines == null || lines.isEmpty()) {
            throw new CnabParsingException(
                    "Arquivo CNAB400 vazio ou nulo: '%s'".formatted(originalFileName)
            );
        }

        String firstLine = lines.getFirst();
        BankCode bankCode = BankCode.fromCode(extract(firstLine, 77, 79));
        CnabFile cnabFile = CnabFile.receive(originalFileName, CnabType.CNAB400, bankCode);

        int processed = 0;
        int rejected  = 0;

        for (int i = 0; i < lines.size(); i++) {
            int    lineNumber = i + 1;
            String line       = lines.get(i);

            if (line.length() != LINE_LENGTH) {
                log.warn("Linha {} com tamanho inválido: {} (esperado {})",
                        lineNumber, line.length(), LINE_LENGTH);
                rejected++;
                continue;
            }

            if (DETAIL_RECORD.equals(extract(line, 1, 1))) {
                try {
                    cnabFile.addTransaction(parseDetail(line, lineNumber));
                    processed++;
                } catch (Exception e) {
                    log.warn("Falha ao parsear detalhe CNAB400 — linha {}: {}",
                            lineNumber, e.getMessage());
                    rejected++;
                }
            }
        }

        cnabFile.registerProcessingResult(processed, rejected);

        log.info("Parse CNAB400 concluído — banco: {}, processados: {}, rejeitados: {}",
                bankCode.getCode(), processed, rejected);

        return cnabFile;
    }

    private TransactionRecord parseDetail(String line, int lineNumber) {
        return TransactionRecord.builder()
                .lineNumber(lineNumber)
                .payerDocument(extract(line, 3, 16))
                .beneficiaryAgency(extract(line, 17, 20))
                .beneficiaryAccount(extract(line, 22, 30))
                .beneficiaryName(extract(line, 63, 76))
                .documentNumber(extract(line, 117, 126))
                .paymentDate(extractDate(line, 147, 154))
                .amount(extractAmount(line, 127, 139))
                .currencyType("BRL")
                .rawLine(line)
                .build();
    }
}