package com.seuportfolio.cnab_processor.domain.model;

/**
 * Define os limites (0-indexed, end exclusivo) de um campo em uma linha CNAB.
 * Compatível com String.substring(start, end).
 */
public record FieldRange(int start, int end) {
    public int length() { return end - start; }
}