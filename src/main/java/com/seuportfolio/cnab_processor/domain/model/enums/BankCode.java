package com.seuportfolio.cnab_processor.domain.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BankCode {

    BB("001", "Banco do Brasil"),
    ITAU("341", "Itaú Unibanco"),
    BRADESCO("237", "Banco Bradesco"),
    CAIXA("104", "Caixa Econômica Federal"),
    SANTANDER("033", "Banco Santander");

    private final String code;
    private final String name;

    public static BankCode fromCode(String code) {
        for (BankCode bank : values()) {
            if (bank.code.equals(code)) return bank;
        }
        throw new IllegalArgumentException(
                "Código de banco não suportado: '%s'. Bancos disponíveis: 001, 237, 341."
                        .formatted(code)
        );
    }
}