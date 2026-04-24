package com.seuportfolio.cnab_processor.domain.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Módulo 11 — Dígito Verificador")
class Modulo11ValidatorTest {

    private final Modulo11Validator validator = new Modulo11Validator();

    @Nested
    @DisplayName("Variante FEBRABAN")
    class VarianteFebraban {

        @ParameterizedTest(name = "[{index}] número={0} → DV esperado={1}")
        @CsvSource({
                "123456789, 7",   // era 8 — corrigido
                "001900450, 8",   // era 0 — corrigido (resto=3, DV=11-3=8)
                "987654321, 6",   // era 1 — corrigido
        })
        @DisplayName("Deve calcular DV corretamente")
        void deveCalcular(String numero, int dvEsperado) {
            assertThat(validator.calculate(numero.trim()))
                    .as("DV FEBRABAN de '%s'", numero.trim())
                    .isEqualTo(dvEsperado);
        }
    }

    @Nested
    @DisplayName("Variante Banco do Brasil")
    class VarianteBB {

        @Test
        @DisplayName("Deve retornar 1 apenas quando resto for 0 ou 1")
        void deveRetornarUmSoParaRestoPequeno() {
            // "10" → 0×2 + 1×3 = 3, resto=3 → DV=8 (não entra na regra especial BB)
            // Precisamos de um número onde soma%11 < 2
            // "100" → 0×2 + 0×3 + 1×4 = 4, resto=4 → não
            // "10000000000" → 0s + 1×2 = 2 ... vamos usar número com soma=11 → resto=0
            // soma=11: ex "20" → 0×2+2×3=6, não. Calculando: preciso de soma=11
            // "29" → 9×2+2×3=18+6=24, não. Vamos usar soma=22: "49"→9×2+4×3=18+12=30, não
            // Melhor: construir programaticamente
            String numero = "29";    // 9×2=18, 2×3=6, soma=24, 24%11=2 → DV=9 (não é caso especial)
            // Para resto=0: soma divisível por 11. Exemplo: "38" → 8×2+3×3=16+9=25, não
            // "011" → 1×2+1×3+0×4=2+3=5, não
            // Vou usar um número que sei que produz resto=0 ou 1
            // "2" → 2×2=4, 4%11=4 → não
            // Testar com "11" → 1×2+1×3=2+3=5 → não
            // "1000000000" → 0×2+...+1×2=2, resto=2 → não
            // Abordagem correta: calcular pelo método e verificar auto-consistência
            int dv = validator.calculate("123456789");  // dv=7, resto=4
            // Para BB onde resto<2: vamos usar "0" → 0×2=0, soma=0, 0%11=0 → DV=1 (BB)
            assertThat(validator.calculate("0", Modulo11Validator.Variant.BANCO_DO_BRASIL))
                    .isEqualTo(1);
            assertThat(validator.calculate("0", Modulo11Validator.Variant.FEBRABAN))
                    .isEqualTo(0);
        }

        @Test
        @DisplayName("Deve convergir com FEBRABAN quando resto >= 2")
        void deveConvergirComFebrabranParaRestoAlto() {
            // Para "123456789" resto=4 (>=2), ambas as variantes dão 11-4=7
            assertThat(validator.calculate("123456789", Modulo11Validator.Variant.BANCO_DO_BRASIL))
                    .isEqualTo(validator.calculate("123456789", Modulo11Validator.Variant.FEBRABAN));
        }
    }

    @Test
    @DisplayName("Deve validar número com DV correto")
    void deveValidar() {
        String numero = "123456789";
        int dv = validator.calculate(numero);  // dv=7
        assertThat(validator.validate(numero + dv)).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar DV incorreto")
    void deveRejeitarDvIncorreto() {
        assertThat(validator.validate("1234567890")).isFalse();
    }

    @Test
    @DisplayName("Deve lançar exceção para string vazia")
    void deveLancarExcecaoParaVazio() {
        assertThatThrownBy(() -> validator.calculate(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nula ou vazia");
    }
}