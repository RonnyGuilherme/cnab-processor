package com.seuportfolio.cnab_processor.application.service;

import com.seuportfolio.cnab_processor.application.service.CnabLayoutLoader;
import com.seuportfolio.cnab_processor.domain.model.CnabFile;
import com.seuportfolio.cnab_processor.domain.model.CnabLayout;
import com.seuportfolio.cnab_processor.domain.model.ParseResult;
import com.seuportfolio.cnab_processor.domain.model.TransactionRecord;
import com.seuportfolio.cnab_processor.domain.model.enums.BankCode;
import com.seuportfolio.cnab_processor.domain.model.enums.CnabType;
import com.seuportfolio.cnab_processor.domain.model.enums.TransactionStatus;
import com.seuportfolio.cnab_processor.domain.service.CnabParser;
import com.seuportfolio.cnab_processor.infrastructure.exception.CnabParsingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Parser CNAB 240 em modo streaming.
 *
 * <p><b>Segmentos suportados:</b></p>
 * <ul>
 *   <li>A — TED/DOC/transferência (cria TransactionRecord)</li>
 *   <li>B — Complemento do favorecido (enriquece registro A pendente)</li>
 *   <li>J — Pagamento de boleto (cria TransactionRecord)</li>
 *   <li>C, N, O, outros — ignorados (SKIP)</li>
 * </ul>
 *
 * <p><b>Lookahead de 1 registro:</b> segmento B pode enriquecer o segmento A
 * anterior. O registro A fica pendente ({@code pendingRecord}) até que um novo
 * A, J ou o fim do arquivo seja encontrado.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cnab240ParserImpl implements CnabParser {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final int EXPECTED_LENGTH = 240;

    private final CnabLayoutLoader layoutLoader;

    /** Registro A/J ainda não emitido — aguarda possível segmento B. */
    private TransactionRecord pendingRecord = null;
    private CnabLayout layout;

    @Override
    public CnabType supportedType() {
        return CnabType.CNAB240;
    }

    @Override
    public CnabFile parseHeader(String line, String fileName) {
        if (line == null || line.length() < 3) {
            throw new CnabParsingException("Header CNAB 240 inválido: linha muito curta.");
        }

        String bankCodeStr = line.substring(0, 3).trim();
        BankCode bankCode  = BankCode.fromCode(bankCodeStr).orElse(null);

        // Carrega layout: específico por banco ou default
        String format = "cnab240";
        layout = layoutLoader.getLayout(format, bankCodeStr);
        if (layout == null) {
            log.warn("Layout '{}' não encontrado, usando default", format + "-" + bankCodeStr);
            layout = layoutLoader.getLayout(format, "default");
        }

        CnabFile cnabFile = CnabFile.receive(fileName, CnabType.CNAB240, bankCode);

        log.info("Header CNAB240 — banco: {}, layout: {}", bankCodeStr, layout.bankCode());
        return cnabFile;
    }

    @Override
    public ParseResult parseLine(String line, int lineNumber, CnabFile cnabFile) {
        if (line == null || line.length() != EXPECTED_LENGTH) {
            log.warn("Linha {} com tamanho inválido: {} (esperado {})",
                    lineNumber, line == null ? 0 : line.length(), EXPECTED_LENGTH);
            return ParseResult.skip();
        }

        char recordType = line.charAt(7);
        char segment    = line.charAt(13);

        return switch (recordType) {
            case '0' -> ParseResult.skip(); // header arquivo
            case '1' -> ParseResult.skip(); // header lote
            case '5' -> ParseResult.skip(); // trailer lote
            case '9' -> ParseResult.skip(); // trailer arquivo
            case '3' -> parseDetail(line, lineNumber, segment, cnabFile);
            default  -> {
                log.debug("Linha {} — tipo de registro desconhecido: '{}'", lineNumber, recordType);
                yield ParseResult.skip();
            }
        };
    }

    @Override
    public ParseResult flush() {
        if (pendingRecord != null) {
            TransactionRecord result = pendingRecord;
            pendingRecord = null;
            log.debug("Flush — emitindo último registro pendente (linha {})", result.getLineNumber());
            return ParseResult.ofSegmentA(result);
        }
        return ParseResult.skip();
    }

    // ── Parse por segmento ────────────────────────────────────────────────────

    private ParseResult parseDetail(String line, int lineNumber, char segment, CnabFile cnabFile) {
        return switch (segment) {
            case 'A' -> {
                // Emite o pendingRecord anterior antes de criar o novo
                TransactionRecord previous = pendingRecord;
                pendingRecord = parseSegmentA(line, lineNumber, cnabFile);

                if (previous != null) {
                    yield ParseResult.ofSegmentA(previous);
                }
                yield ParseResult.skip(); // novo A vai para pending, não emite ainda
            }
            case 'B' -> {
                if (pendingRecord != null) {
                    enrichWithSegmentB(line, pendingRecord);
                    log.debug("Segmento B — endereço enriquecido na linha {}", lineNumber);
                } else {
                    log.warn("Segmento B na linha {} sem registro A precedente", lineNumber);
                }
                yield ParseResult.ofSegmentB(null); // enriquecimento direto, sem novo record
            }
            case 'J' -> {
                // Emite A pendente (se houver) e cria J como novo pending
                TransactionRecord previous = pendingRecord;
                pendingRecord = parseSegmentJ(line, lineNumber, cnabFile);

                if (previous != null) {
                    yield ParseResult.ofSegmentA(previous); // emite o A pendente primeiro
                }
                yield ParseResult.skip(); // J vai para pending
            }
            case 'C', 'N', 'O' -> {
                log.debug("Segmento '{}' na linha {} ignorado (não implementado)", segment, lineNumber);
                yield ParseResult.skip();
            }
            default -> {
                log.debug("Segmento desconhecido '{}' na linha {}", segment, lineNumber);
                yield ParseResult.skip();
            }
        };
    }

    private TransactionRecord parseSegmentA(String line, int lineNumber, CnabFile cnabFile) {
        TransactionRecord record = new TransactionRecord();
        record.setLineNumber(lineNumber);
        record.setRawLine(line);
        record.setSegmentType("A");
        record.setStatus(TransactionStatus.PENDING);

        try {
            record.setBeneficiaryName(extract(line, "A", "beneficiaryName"));
            record.setBeneficiaryAgency(extract(line, "A", "beneficiaryAgency"));
            record.setBeneficiaryAccount(extract(line, "A", "beneficiaryAccount"));
            record.setDocumentNumber(extract(line, "A", "documentNumber"));
            record.setCurrencyType(extract(line, "A", "currencyType"));
            record.setPayerDocument(extract(line, "A", "payerDocument"));

            String amountStr  = extract(line, "A", "amount").replaceAll("\\D", "");
            record.setAmount(amountStr.isBlank() ? BigDecimal.ZERO
                    : new BigDecimal(amountStr).movePointLeft(2));

            String dateStr = extract(line, "A", "paymentDate");
            record.setPaymentDate(parseDate(dateStr, lineNumber));

            record.associateTo(cnabFile);
        } catch (Exception e) {
            log.warn("Erro ao parsear segmento A na linha {}: {}", lineNumber, e.getMessage());
            record.reject("Erro de parse: " + e.getMessage());
        }

        return record;
    }

    private TransactionRecord parseSegmentJ(String line, int lineNumber, CnabFile cnabFile) {
        TransactionRecord record = new TransactionRecord();
        record.setLineNumber(lineNumber);
        record.setRawLine(line);
        record.setSegmentType("J");
        record.setStatus(TransactionStatus.PENDING);

        try {
            record.setBarCode(extract(line, "J", "barCode"));
            record.setAssignorName(extract(line, "J", "assignorName"));
            record.setBeneficiaryName(extract(line, "J", "beneficiaryName"));
            record.setDocumentNumber(extract(line, "J", "documentNumber"));

            String amountStr = extract(line, "J", "amount").replaceAll("\\D", "");
            record.setAmount(amountStr.isBlank() ? BigDecimal.ZERO
                    : new BigDecimal(amountStr).movePointLeft(2));

            String dateStr = extract(line, "J", "paymentDate");
            record.setPaymentDate(parseDate(dateStr, lineNumber));

            record.associateTo(cnabFile);
        } catch (Exception e) {
            log.warn("Erro ao parsear segmento J na linha {}: {}", lineNumber, e.getMessage());
            record.reject("Erro de parse boleto: " + e.getMessage());
        }

        return record;
    }

    private void enrichWithSegmentB(String line, TransactionRecord record) {
        record.setBeneficiaryAddress(extract(line, "B", "beneficiaryAddress"));
        record.setBeneficiaryCity(extract(line, "B", "beneficiaryCity"));
        record.setBeneficiaryState(extract(line, "B", "beneficiaryState"));
        record.setBeneficiaryCep(extract(line, "B", "beneficiaryCep"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extract(String line, String segment, String field) {
        if (layout == null) return "";
        return layout.extract(line, segment, field);
    }

    private LocalDate parseDate(String dateStr, int lineNumber) {
        if (dateStr == null || dateStr.isBlank() || dateStr.equals("00000000")) return null;
        try {
            return LocalDate.parse(dateStr.trim(), DATE_FMT);
        } catch (DateTimeParseException e) {
            log.debug("Data inválida na linha {}: '{}'", lineNumber, dateStr);
            return null;
        }
    }
}