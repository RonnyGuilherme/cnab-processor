package com.seuportfolio.cnab_processor.domain.model;

import com.seuportfolio.cnab_processor.domain.model.enums.ParseResultType;
import org.springframework.lang.Nullable;

/**
 * Resultado do parse de uma linha CNAB pelo parser em modo streaming.
 *
 * <p>Um resultado pode ser:</p>
 * <ul>
 *   <li>SKIP — linha irrelevante (header, trailer, segmento não suportado)</li>
 *   <li>SEGMENT_A — nova transação de crédito/TED</li>
 *   <li>SEGMENT_B — complemento da transação anterior (endereço)</li>
 *   <li>SEGMENT_J — pagamento de boleto</li>
 * </ul>
 */
public record ParseResult(ParseResultType type, @Nullable TransactionRecord record) {

    public static ParseResult skip() {
        return new ParseResult(ParseResultType.SKIP, null);
    }

    public static ParseResult ofSegmentA(TransactionRecord record) {
        return new ParseResult(ParseResultType.SEGMENT_A, record);
    }

    public static ParseResult ofSegmentB(TransactionRecord enrichment) {
        return new ParseResult(ParseResultType.SEGMENT_B, enrichment);
    }

    public static ParseResult ofSegmentJ(TransactionRecord record) {
        return new ParseResult(ParseResultType.SEGMENT_J, record);
    }
}
