package com.seuportfolio.cnab_processor.application.service.banco;

import com.seuportfolio.cnab_processor.domain.model.enums.BankCode;
import com.seuportfolio.cnab_processor.domain.validator.Modulo10Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ItauStrategy — Validação de agência e conta")
class ItauStrategyTest {

    private final Modulo10Validator validator = new Modulo10Validator();
    private final ItauStrategy strategy = new ItauStrategy(validator);

    @Test
    @DisplayName("Deve suportar o banco Itaú (código 341)")
    void deveSupportarItau() {
        assertThat(strategy.supportedBank()).isEqualTo(BankCode.ITAU);
    }

    @Test
    @DisplayName("Deve validar DV calculado sobre agência + conta concatenados")
    void deveValidarDvConcatenado() {
        // Itaú: DV sobre os 9 dígitos de agência(4) + conta(5)
        String agency  = "1234";
        String account = "56789";
        String base    = agency + account;          // "123456789"
        String dv      = String.valueOf(validator.calculate(base));

        assertThat(strategy.validateBankDetails(agency, account, "", dv)).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar DV incorreto")
    void deveRejeitarDvIncorreto() {
        assertThat(strategy.validateBankDetails("1234", "56789", "", "0")).isFalse();
    }
}