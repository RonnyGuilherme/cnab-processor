package com.seuportfolio.cnab_processor.domain.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CnabType {

    CNAB240(240, "CNAB 240 posições — padrão FEBRABAN para pagamentos"),
    CNAB400(400, "CNAB 400 posições — padrão FEBRABAN para cobranças");

    private final int lineLength;
    private final String description;

    public static CnabType detect(int lineLength) {
        for (CnabType type : values()) {
            if (type.lineLength == lineLength) return type;
        }
        throw new IllegalArgumentException(
                "Tamanho de linha não corresponde a nenhum padrão CNAB suportado: %d. Esperado: 240 ou 400."
                        .formatted(lineLength)
        );
    }
}