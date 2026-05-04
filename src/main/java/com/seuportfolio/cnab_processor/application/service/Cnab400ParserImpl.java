package com.seuportfolio.cnab_processor.application.service;

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

@Slf4j
@Component
@RequiredArgsConstructor
public class Cnab400ParserImpl implements CnabParser {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final int EXPECTED_LENGTH = 400;

    private final CnabLayoutLoader layoutLoader;
    private CnabLayout layout;

    @Override
    public CnabType supportedType() {
        return CnabType.CNAB400;
    }

    @Override
    public CnabFile parseHeader(String line, String fileName) {
        if (line == null || line.length() < 3) {
            throw new CnabParsingException("Header CNAB 400 inválido.");
        }

        String bankCodeStr = line.substring(76, 79).trim();
        BankCode bankCode = BankCode.fromCode(bankCodeStr).orElse(null);

        layout = layoutLoader.getLayout("cnab400", bankCodeStr);
        if (layout == null) {
            layout = layoutLoader.getLayout("cnab400", "default");
        }

        CnabFile cnabFile = CnabFile.receive(fileName, CnabType.CNAB400, bankCode);
        log.info("Header CNAB400 — banco: {}", bankCode);
        return cnabFile;   // ← ESSENCIAL
    }


    @Override
    public ParseResult parseLine(String line, int lineNumber, CnabFile cnabFile) {
        if (line == null || line.length() != EXPECTED_LENGTH) {
            log.warn("Linha {} com tamanho inválido: {}", lineNumber,
                    line == null ? 0 : line.length());
            return ParseResult.skip();
        }

        char recordType = line.charAt(0);
        if (recordType == '0' || recordType == '9') return ParseResult.skip();

        TransactionRecord record = new TransactionRecord();
        record.setLineNumber(lineNumber);
        record.setRawLine(line);
        record.setSegmentType("DETAIL");
        record.setStatus(TransactionStatus.PENDING);

        try {
            record.setBeneficiaryAgency(layout.extract(line, "DETAIL", "beneficiaryAgency"));
            record.setBeneficiaryAccount(layout.extract(line, "DETAIL", "beneficiaryAccount"));
            record.setBeneficiaryName(layout.extract(line, "DETAIL", "beneficiaryName"));
            record.setDocumentNumber(layout.extract(line, "DETAIL", "documentNumber"));
            record.setPayerDocument(layout.extract(line, "DETAIL", "payerDocument"));

            String currency = layout.extract(line, "DETAIL", "currencyType");
            record.setCurrencyType(currency.isBlank() ? "BRL" : currency);

            String amountStr = layout.extract(line, "DETAIL", "amount").replaceAll("\\D", "");
            record.setAmount(amountStr.isBlank() ? BigDecimal.ZERO
                    : new BigDecimal(amountStr).movePointLeft(2));

            String dateStr = layout.extract(line, "DETAIL", "paymentDate");
            record.setPaymentDate(parseDate(dateStr, lineNumber));

            record.associateTo(cnabFile);
        } catch (Exception e) {
            log.warn("Erro ao parsear CNAB400 linha {}: {}", lineNumber, e.getMessage());
            record.reject("Erro de parse: " + e.getMessage());
        }

        return ParseResult.ofSegmentA(record);
    }

    @Override
    public ParseResult flush() { return ParseResult.skip(); }

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