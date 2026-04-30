package com.seuportfolio.cnab_processor.application.service.banco;

import com.seuportfolio.cnab_processor.domain.model.enums.BankCode;
import com.seuportfolio.cnab_processor.domain.validator.Modulo10Validator;
import com.seuportfolio.cnab_processor.domain.validator.Modulo11Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BancoStrategyFactory — Resolução de strategies")
class BancoStrategyFactoryTest {

    private final Modulo10Validator modulo10 = new Modulo10Validator();
    private final Modulo11Validator modulo11 = new Modulo11Validator();

    private final BancoStrategyFactory factory = new BancoStrategyFactory(List.of(
            new BancoDoBrasilStrategy(modulo11),
            new ItauStrategy(modulo10),
            new BradescoStrategy(modulo10, modulo11)
    ));

    @Test
    @DisplayName("Deve resolver strategy do BB")
    void deveResolverBB() {
        assertThat(factory.getStrategy(BankCode.BB))
                .isInstanceOf(BancoDoBrasilStrategy.class);
    }

    @Test
    @DisplayName("Deve resolver strategy do Itaú")
    void deveResolverItau() {
        assertThat(factory.getStrategy(BankCode.ITAU))
                .isInstanceOf(ItauStrategy.class);
    }

    @Test
    @DisplayName("Deve resolver strategy do Bradesco")
    void deveResolverBradesco() {
        assertThat(factory.getStrategy(BankCode.BRADESCO))
                .isInstanceOf(BradescoStrategy.class);
    }

    @Test
    @DisplayName("Deve confirmar suporte para bancos registrados")
    void deveConfirmarSuporteParaBancosRegistrados() {
        assertThat(factory.supports(BankCode.BB)).isTrue();
        assertThat(factory.supports(BankCode.ITAU)).isTrue();
        assertThat(factory.supports(BankCode.BRADESCO)).isTrue();
    }

    @Test
    @DisplayName("Deve retornar false para banco sem strategy registrada")
    void deveRetornarFalseParaBancoSemStrategy() {
        assertThat(factory.supports(BankCode.SANTANDER)).isFalse();
    }

    @Test
    @DisplayName("Deve lançar exceção para banco sem strategy registrada")
    void deveLancarExcecaoParaBancoNaoSuportado() {
        assertThatThrownBy(() -> factory.getStrategy(BankCode.SANTANDER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nenhuma strategy para o banco: 033")
                .hasMessageContaining("Suportados:");
    }
}