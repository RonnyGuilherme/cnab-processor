package com.seuportfolio.cnab_processor.domain.service;

import com.seuportfolio.cnab_processor.domain.model.CnabFile;
import com.seuportfolio.cnab_processor.domain.model.ParseResult;
import com.seuportfolio.cnab_processor.domain.model.enums.CnabType;

/**
 * Parser CNAB em modo streaming: lê uma linha por vez.
 *
 * <p><b>Fluxo esperado pelo reader:</b></p>
 * <ol>
 *   <li>{@link #parseHeader} na linha 0 → cria o {@link CnabFile}</li>
 *   <li>{@link #parseLine} para cada linha subsequente</li>
 *   <li>{@link #flush} no fim do arquivo → emite o último registro pendente</li>
 * </ol>
 */
public interface CnabParser {

    CnabType supportedType();

    /** Lê o header do arquivo e cria o CnabFile de metadados. */
    CnabFile parseHeader(String headerLine, String fileName);

    /**
     * Processa uma linha e retorna o resultado.
     * Pode retornar SKIP para linhas que não geram transação diretamente
     * (ex: header de lote, trailer, segmento B que enriquece o registro anterior).
     */
    ParseResult parseLine(String line, int lineNumber, CnabFile cnabFile);

    /**
     * Chamado no fim do arquivo para emitir qualquer registro pendente
     * que ainda não foi retornado (ex: último segmento A sem B subsequente).
     */
    ParseResult flush();
}