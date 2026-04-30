package com.seuportfolio.cnab_processor.application.service.banco;

import com.seuportfolio.cnab_processor.domain.model.enums.BankCode;
import com.seuportfolio.cnab_processor.domain.service.BancoStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolve a {@link BancoStrategy} correta para cada código de banco.
 * Segue o mesmo padrão da {@code CnabParserFactory}: o Spring injeta todas
 * as implementações disponíveis e a factory indexa por {@link BankCode}.
 * Adicionar suporte a um novo banco exige apenas um novo {@code @Component}
 * — nenhuma alteração aqui (Open/Closed Principle).
 */
@Slf4j
@Component
public class BancoStrategyFactory {

    private final Map<BankCode, BancoStrategy> strategies;

    public BancoStrategyFactory(List<BancoStrategy> availableStrategies) {
        this.strategies = availableStrategies.stream()
                .collect(Collectors.toUnmodifiableMap(
                        BancoStrategy::supportedBank,
                        Function.identity()
                ));

        log.info("BancoStrategyFactory inicializada — strategies registradas: {}", strategies.keySet());
    }

    /**
     * Retorna a strategy do banco informado.
     *
     * @param bankCode código do banco
     * @return strategy correspondente
     * @throws IllegalArgumentException se não houver strategy registrada
     */
    public BancoStrategy getStrategy(BankCode bankCode) {
        return Optional.ofNullable(strategies.get(bankCode))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nenhuma strategy para o banco: " + bankCode.getCode()
                                + ". Suportados: " + strategies.keySet().stream()
                                .map(BankCode::getCode)
                                .sorted()
                                .collect(Collectors.joining(", "))));
    }

    /**
     * Verifica se há strategy disponível para o banco, sem lançar exceção.
     * Útil para decidir se a validação de DV deve ser aplicada.
     */
    public boolean supports(BankCode bankCode) {
        return strategies.containsKey(bankCode);
    }
}