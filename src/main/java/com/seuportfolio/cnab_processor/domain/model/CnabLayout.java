package com.seuportfolio.cnab_processor.domain.model;

import java.util.Map;

/**
 * Layout completo de um formato CNAB para um banco específico.
 * Carregado a partir dos arquivos YAML em resources/layouts/.
 */
public record CnabLayout(
        String bankCode,
        String version,
        Map<String, Map<String, FieldRange>> segments  // segmento → (campo → range)
) {
    /** Extrai um campo de uma linha usando o layout do segmento informado. */
    public String extract(String line, String segment, String field) {
        Map<String, FieldRange> segFields = segments.get(segment);
        if (segFields == null) return "";
        FieldRange range = segFields.get(field);
        if (range == null || range.end() > line.length()) return "";
        return line.substring(range.start(), range.end()).trim();
    }
}