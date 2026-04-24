package com.seuportfolio.cnab_processor.application.service.banco;

import com.seuportfolio.cnab_processor.domain.model.enums.BankCode;
import com.seuportfolio.cnab_processor.domain.validator.Modulo11Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BancoDoBrasilStrategy — Validação de agência e conta")
class BancoDoBrasilStrategyTest {

    private final Modulo11Validator validator = new Modulo11Validator();
    private final BancoDoBrasilStrategy strategy = new BancoDoBrasilStrategy(validator);

    @Test
    @DisplayName("Deve suportar o banco BB (código 001)")
    void deveSupportarBB() {
        assertThat(strategy.supportedBank()).isEqualTo(BankCode.BB);
    }

    @Test
    @DisplayName("Deve validar agência e conta com DV correto pela variante BB")
    void deveValidarAgenciaContaCorretos() {
        // Constrói DVs válidos pela variante BB
        String agency  = "1234";
        String account = "56789012";
        String agencyDv  = String.valueOf(validator.calculate(agency,  Modulo11Validator.Variant.BANCO_DO_BRASIL));
        String accountDv = String.valueOf(validator.calculate(account, Modulo11Validator.Variant.BANCO_DO_BRASIL));

        assertThat(strategy.validateBankDetails(agency, account, agencyDv, accountDv)).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar DV de agência incorreto")
    void deveRejeitarDvAgenciaIncorreto() {
        assertThat(strategy.validateBankDetails("1234", "56789012", "9", "0")).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar DV de conta incorreto")
    void deveRejeitarDvContaIncorreto() {
        String agency   = "1234";
        String agencyDv = String.valueOf(validator.calculate(agency, Modulo11Validator.Variant.BANCO_DO_BRASIL));
        assertThat(strategy.validateBankDetails(agency, "56789012", agencyDv, "9")).isFalse();
    }
}
