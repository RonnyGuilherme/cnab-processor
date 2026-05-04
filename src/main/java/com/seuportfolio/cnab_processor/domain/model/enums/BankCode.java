package com.seuportfolio.cnab_processor.domain.model.enums;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;


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

    public static Optional<BankCode> fromCode(String code) {
        return Arrays.stream(values())
                .filter(b -> b.getCode().equals(code))
                .findFirst();
    }


public class CnabFile {
    private UUID id;
    private String fileName;
    private String originalFileName;
    private CnabType cnabType;
    private BankCode bankCode;
    private int processedLines;
    private int rejectedLines;

}

}