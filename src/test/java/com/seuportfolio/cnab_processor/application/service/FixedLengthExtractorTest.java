package com.seuportfolio.cnab_processor.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("FixedLengthExtractor — Extração de campos CNAB")
class FixedLengthExtractorTest {

    // Header CNAB240 sintético: 240 caracteres exatos
    private static final String HEADER = (
            "001"                          +  // pos 1-3   banco
                    "0000"                         +  // pos 4-7   lote
                    "0"                            +  // pos 8     tipo
                    " ".repeat(9)                  +  // pos 9-17
                    "2"                            +  // pos 18    tipo inscrição
                    "12345678000199"               +  // pos 19-32 CNPJ
                    " ".repeat(10)                 +  // pos 33-42
                    "00190"                        +  // pos 43-47 agência
                    " "                            +  // pos 48
                    "EMPRESA TESTE LTDA SA         " + // pos 49-78 nome
                    " ".repeat(162)                   // pos 79-240
    );

    @Test
    @DisplayName("Deve extrair código do banco (pos 1–3)")
    void deveExtrairBanco() {
        assertThat(FixedLengthExtractor.extract(HEADER, 1, 3)).isEqualTo("001");
    }

    @Test
    @DisplayName("Deve fazer trim do campo extraído")
    void deveFazerTrim() {
        assertThat(FixedLengthExtractor.extract(HEADER, 49, 78))
                .isEqualTo("EMPRESA TESTE LTDA SA");
    }

    @Test
    @DisplayName("Deve extrair campo como int")
    void deveExtrairInt() {
        String linha = "00000123" + " ".repeat(232);
        assertThat(FixedLengthExtractor.extractInt(linha, 1, 8)).isEqualTo(123);
    }

    @Test
    @DisplayName("Deve retornar 0 para int em branco")
    void deveRetornarZeroParaIntEmBranco() {
        String linha = " ".repeat(240);
        assertThat(FixedLengthExtractor.extractInt(linha, 1, 8)).isEqualTo(0);
    }

    @Test
    @DisplayName("Deve extrair valor monetário — R$ 123,50 representado como '00000000012350'")
    void deveExtrairValorMonetario() {
        String linha = "00000000012350" + " ".repeat(226);
        assertThat(FixedLengthExtractor.extractAmount(linha, 1, 14))
                .isEqualByComparingTo(new BigDecimal("123.50"));
    }

    @Test
    @DisplayName("Deve retornar ZERO para campo monetário zerado")
    void deveRetornarZeroMonetario() {
        String linha = "00000000000000" + " ".repeat(226);
        assertThat(FixedLengthExtractor.extractAmount(linha, 1, 14))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve extrair data no formato DDMMAAAA")
    void deveExtrairData() {
        String linha = "15032024" + " ".repeat(232);
        assertThat(FixedLengthExtractor.extractDate(linha, 1, 8))
                .isEqualTo(LocalDate.of(2024, 3, 15));
    }

    @Test
    @DisplayName("Deve retornar null para data zerada")
    void deveRetornarNullParaDataZerada() {
        String linha = "00000000" + " ".repeat(232);
        assertThat(FixedLengthExtractor.extractDate(linha, 1, 8)).isNull();
    }

    @Test
    @DisplayName("Deve lançar exceção para posição final além do tamanho da linha")
    void deveLancarExcecaoParaPosicaoInvalida() {
        assertThatThrownBy(() -> FixedLengthExtractor.extract(HEADER, 1, 300))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("excede o tamanho da linha");
    }

    @Test
    @DisplayName("Deve lançar exceção para linha nula")
    void deveLancarExcecaoParaNulo() {
        assertThatThrownBy(() -> FixedLengthExtractor.extract(null, 1, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nula");
    }

    @Test
    @DisplayName("Deve lançar exceção para posição inicial menor que 1")
    void deveLancarExcecaoParaPosicaoZero() {
        assertThatThrownBy(() -> FixedLengthExtractor.extract(HEADER, 0, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(">= 1");
    }
}