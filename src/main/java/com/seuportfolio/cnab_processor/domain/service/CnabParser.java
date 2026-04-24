package com.seuportfolio.cnab_processor.domain.service;

import com.seuportfolio.cnab_processor.domain.model.CnabFile;
import com.seuportfolio.cnab_processor.domain.model.enums.CnabType;

import java.util.List;

/**
 * Contrato para parsing de arquivos CNAB.
 * A escolha da implementação correta é responsabilidade do CnabParserFactory.
 */
public interface CnabParser {

    /** Formato que esta implementação suporta — usado pela factory para roteamento. */
    CnabType supportedType();

    /**
     * @param lines            linhas brutas do arquivo, sem modificação
     * @param originalFileName nome original do arquivo recebido
     * @return {@link CnabFile} populado com as transações extraídas
     */
    CnabFile parse(List<String> lines, String originalFileName);
}