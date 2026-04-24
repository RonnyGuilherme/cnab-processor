package com.seuportfolio.cnab_processor.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Extração de campos de arquivos CNAB de posição fixa.
 *
 * Convenção FEBRABAN: posições 1-based, inclusivo em ambas as extremidades.
 * Classe sem estado e sem anotações Spring — testável de forma completamente isolada.
 */
public final class FixedLengthExtractor {

    private static final DateTimeFormatter CNAB_DATE = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final String ZERO_DATE            = "00000000";

    private FixedLengthExtractor() {
        throw new UnsupportedOperationException("Classe utilitária.");
    }

    /** Extrai e faz trim de um campo textual (posições 1-based, inclusivas). */
    public static String extract(String line, int start, int end) {
        validate(line, start, end);
        return line.substring(start - 1, end).trim();
    }

    /** Extrai campo como {@code int}. Retorna 0 para campo em branco. */
    public static int extractInt(String line, int start, int end) {
        String value = extract(line, start, end);
        return value.isEmpty() ? 0 : Integer.parseInt(value);
    }

    /** Extrai campo como {@code long}. Retorna 0 para campo em branco. */
    public static long extractLong(String line, int start, int end) {
        String value = extract(line, start, end);
        return value.isEmpty() ? 0L : Long.parseLong(value);
    }

    /**
     * Extrai valor monetário CNAB (inteiro com decimais implícitos).
     * R$ 123,50 é representado como "0000000012350" com {@code decimalPlaces = 2}.
     */
    public static BigDecimal extractAmount(String line, int start, int end, int decimalPlaces) {
        String value = extract(line, start, end);
        if (value.isEmpty() || value.matches("0+")) return BigDecimal.ZERO;
        return new BigDecimal(value).divide(BigDecimal.TEN.pow(decimalPlaces));
    }

    /** Extrai valor monetário com 2 casas decimais (padrão FEBRABAN). */
    public static BigDecimal extractAmount(String line, int start, int end) {
        return extractAmount(line, start, end, 2);
    }

    /**
     * Extrai data no formato {@code DDMMAAAA}.
     * Retorna {@code null} para campo zerado ("00000000") ou vazio.
     */
    public static LocalDate extractDate(String line, int start, int end) {
        String value = extract(line, start, end);
        if (value.isEmpty() || ZERO_DATE.equals(value)) return null;
        try {
            return LocalDate.parse(value, CNAB_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Data inválida no campo [%d–%d]: '%s'".formatted(start, end, value), e
            );
        }
    }

    private static void validate(String line, int start, int end) {
        if (line == null) {
            throw new IllegalArgumentException("Linha CNAB não pode ser nula.");
        }
        if (start < 1) {
            throw new IllegalArgumentException(
                    "Posição inicial deve ser >= 1, recebido: %d.".formatted(start)
            );
        }
        if (end < start) {
            throw new IllegalArgumentException(
                    "Posição final (%d) deve ser >= posição inicial (%d).".formatted(end, start)
            );
        }
        if (end > line.length()) {
            throw new IllegalArgumentException(
                    "Posição final (%d) excede o tamanho da linha (%d).".formatted(end, line.length())
            );
        }
    }
}