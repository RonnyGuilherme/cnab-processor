package com.seuportfolio.cnab_processor.domain.validator;

import org.springframework.stereotype.Component;

/**
 * Dígito verificador pelo algoritmo Módulo 11 (FEBRABAN).
 *
 * Algoritmo padrão FEBRABAN:
 *  1. Multiplica os dígitos por pesos de 2 a 9, da direita para a esquerda, ciclicamente.
 *  2. Soma todos os produtos.
 *  3. Resto = soma % 11
 *  4. Resto < 2 → DV = 0; caso contrário → DV = 11 − resto
 *
 * Variante BB: resto < 2 → DV = 1 (em vez de 0).
 *
 * Uso típico: código de barras CNAB, nosso número, convênio.
 */
@Component
public class Modulo11Validator {

    public enum Variant {
        FEBRABAN,       // resto < 2 → DV = 0
        BANCO_DO_BRASIL // resto < 2 → DV = 1
    }

    public int calculate(String number) {
        return calculate(number, Variant.FEBRABAN);
    }

    public int calculate(String number, Variant variant) {
        String digits = sanitize(number);

        int sum    = 0;
        int weight = 2;

        for (int i = digits.length() - 1; i >= 0; i--) {
            sum   += Character.getNumericValue(digits.charAt(i)) * weight;
            weight = (weight == 9) ? 2 : weight + 1;
        }

        int remainder = sum % 11;

        return switch (variant) {
            case FEBRABAN        -> (remainder < 2) ? 0 : 11 - remainder;
            case BANCO_DO_BRASIL -> (remainder < 2) ? 1 : 11 - remainder;
        };
    }

    public boolean validate(String numberWithDv) {
        return validate(numberWithDv, Variant.FEBRABAN);
    }

    public boolean validate(String numberWithDv, Variant variant) {
        if (numberWithDv == null || numberWithDv.length() < 2) return false;

        String withoutDv = numberWithDv.substring(0, numberWithDv.length() - 1);
        int givenDv     = Character.getNumericValue(numberWithDv.charAt(numberWithDv.length() - 1));

        return calculate(withoutDv, variant) == givenDv;
    }

    private String sanitize(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException(
                    "Entrada não pode ser nula ou vazia para cálculo Módulo 11."
            );
        }
        String digits = input.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            throw new IllegalArgumentException(
                    "Entrada não contém dígitos válidos: '%s'".formatted(input)
            );
        }
        return digits;
    }
}