package com.seuportfolio.cnab_processor.infrastructure.exception;

/**
 * Lançada quando um arquivo CNAB está malformado ou não pode ser parseado.
 */
public class CnabParsingException extends RuntimeException {

    public CnabParsingException(String message) {
        super(message);
    }

    public CnabParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}