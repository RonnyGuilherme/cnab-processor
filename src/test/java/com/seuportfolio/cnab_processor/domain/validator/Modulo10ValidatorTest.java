package com.seuportfolio.cnab_processor.domain.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Módulo 10 — Dígito Verificador")
class Modulo10ValidatorTest {

    private final Modulo10Validator validator = new Modulo10Validator();

    @ParameterizedTest(name = "[{index}] número={0} → DV esperado={1}")
    @CsvSource({
            "1234,      4",   // era 6 — corrigido
            "00190,     9",   // era 3 — corrigido
            "9999,      4",   // era 8 — corrigido
            "1,         8",   // era 2 — corrigido
            "0000001,   8",   // era 4 — corrigido
            "123456789, 7",   // já estava correto
    })
    @DisplayName("Deve calcular DV corretamente")
    void deveCalcularDvCorretamente(String numero, int dvEsperado) {
        assertThat(validator.calculate(numero.trim()))
                .as("DV de '%s'", numero.trim())
                .isEqualTo(dvEsperado);
    }

    @Test
    @DisplayName("Deve validar número com DV correto")
    void deveValidarNumeroCorreto() {
        // DV de "1234" é 4, então "12344" é o número válido
        assertThat(validator.validate("12344")).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar número com DV incorreto")
    void deveRejeitarDvIncorreto() {
        assertThat(validator.validate("12345")).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false para strings curtas ou nulas")
    void deveRetornarFalseParaEntradaCurta() {
        assertThat(validator.validate(null)).isFalse();
        assertThat(validator.validate("")).isFalse();
        assertThat(validator.validate("5")).isFalse();
    }

    @Test
    @DisplayName("Deve ignorar hífens e espaços")
    void deveIgnorarCaracteresNaoNumericos() {
        assertThat(validator.calculate("123-4"))
                .isEqualTo(validator.calculate("1234"));
    }

    @Test
    @DisplayName("Deve lançar exceção para entrada nula")
    void deveLancarExcecaoParaNulo() {
        assertThatThrownBy(() -> validator.calculate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nula ou vazia");
    }

    @Test
    @DisplayName("Deve lançar exceção para string sem dígitos")
    void deveLancarExcecaoParaSemDigitos() {
        assertThatThrownBy(() -> validator.calculate("---"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não contém dígitos");
    }
}