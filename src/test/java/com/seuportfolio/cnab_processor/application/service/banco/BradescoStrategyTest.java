package com.seuportfolio.cnab_processor.application.service.banco;

import com.seuportfolio.cnab_processor.domain.model.enums.BankCode;
import com.seuportfolio.cnab_processor.domain.validator.Modulo10Validator;
import com.seuportfolio.cnab_processor.domain.validator.Modulo11Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BradescoStrategy — Validação de agência e conta")
class BradescoStrategyTest {

    private final Modulo10Validator modulo10 = new Modulo10Validator();
    private final Modulo11Validator modulo11 = new Modulo11Validator();
    private final BradescoStrategy strategy = new BradescoStrategy(modulo10, modulo11);

    @Test
    @DisplayName("Deve suportar o banco Bradesco (código 237)")
    void deveSupportarBradesco() {
        assertThat(strategy.supportedBank()).isEqualTo(BankCode.BRADESCO);
    }

    @Test
    @DisplayName("Deve validar agência (Módulo 11 FEBRABAN) e conta (Módulo 10) com DVs corretos")
    void deveValidarAgenciaContaCorretos() {
        String agency    = "1234";
        String account   = "1234567";
        String agencyDv  = String.valueOf(modulo11.calculate(agency, Modulo11Validator.Variant.FEBRABAN));
        String accountDv = String.valueOf(modulo10.calculate(account));

        assertThat(strategy.validateBankDetails(agency, account, agencyDv, accountDv)).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar DV de agência incorreto")
    void deveRejeitarDvAgenciaIncorreto() {
        assertThat(strategy.validateBankDetails("1234", "1234567", "9", "0")).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar DV de conta incorreto")
    void deveRejeitarDvContaIncorreto() {
        String agency   = "1234";
        String agencyDv = String.valueOf(modulo11.calculate(agency, Modulo11Validator.Variant.FEBRABAN));
        assertThat(strategy.validateBankDetails(agency, "1234567", agencyDv, "9")).isFalse();
    }
}