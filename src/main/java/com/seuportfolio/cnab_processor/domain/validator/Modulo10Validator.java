package com.seuportfolio.cnab_processor.domain.validator;

import org.springframework.stereotype.Component;

/**
 * Dígito verificador pelo algoritmo Módulo 10 (FEBRABAN).
 *
 * Algoritmo:
 *  1. Multiplica os dígitos alternadamente por 2 e 1, da direita para a esquerda.
 *  2. Produto >= 10 → soma seus algarismos (ex: 16 → 1+6 = 7).
 *  3. Soma todos os resultados.
 *  4. DV = (10 − (soma % 10)) % 10
 *
 * Uso típico: código de barras, linha digitável, agência/conta Bradesco.
 */
@Component
public class Modulo10Validator {

    public int calculate(String number) {
        String digits = sanitize(number);

        int sum = 0;
        int multiplier = 2;

        for (int i = digits.length() - 1; i >= 0; i--) {
            int product = Character.getNumericValue(digits.charAt(i)) * multiplier;
            sum += (product >= 10) ? (product / 10) + (product % 10) : product;
            multiplier = (multiplier == 2) ? 1 : 2;
        }

        return (10 - (sum % 10)) % 10;
    }

    public boolean validate(String numberWithDv) {
        if (numberWithDv == null || numberWithDv.length() < 2) return false;

        String withoutDv = numberWithDv.substring(0, numberWithDv.length() - 1);
        int givenDv     = Character.getNumericValue(numberWithDv.charAt(numberWithDv.length() - 1));

        return calculate(withoutDv) == givenDv;
    }

    private String sanitize(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException(
                    "Entrada não pode ser nula ou vazia para cálculo Módulo 10."
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