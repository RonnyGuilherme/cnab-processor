package com.seuportfolio.cnab_processor.application.service;

import com.seuportfolio.cnab_processor.domain.model.enums.CnabType;
import com.seuportfolio.cnab_processor.domain.service.CnabParser;
import com.seuportfolio.cnab_processor.infrastructure.exception.CnabParsingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolve o CnabParser correto para cada tipo de arquivo.
 *
 * O Spring injeta todas as implementações de CnabParser disponíveis no contexto.
 * Para adicionar suporte a um novo formato, basta criar um @Component que implemente
 * CnabParser — nenhuma alteração aqui é necessária (Open/Closed Principle).
 */
@Slf4j
@Component
public class CnabParserFactory {

    private final Map<CnabType, CnabParser> parsers;

    public CnabParserFactory(List<CnabParser> availableParsers) {
        this.parsers = availableParsers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        CnabParser::supportedType,
                        Function.identity()
                ));

        log.info("CnabParserFactory inicializada — parsers registrados: {}", parsers.keySet());
    }

    public CnabParser getParser(CnabType type) {
        CnabParser parser = parsers.get(type);

        if (parser == null) {
            throw new CnabParsingException(
                    "Nenhum parser registrado para: %s. Disponíveis: %s"
                            .formatted(type, parsers.keySet())
            );
        }

        log.debug("Parser selecionado para {}: {}", type, parser.getClass().getSimpleName());
        return parser;
    }
}