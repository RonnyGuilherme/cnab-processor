package com.seuportfolio.cnab_processor.domain.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransactionStatus {

    PENDING("Aguardando processamento"),
    PROCESSING("Em processamento pelo batch"),
    PROCESSED("Processado com sucesso"),
    REJECTED("Rejeitado por falha de validação"),
    ERROR("Erro inesperado durante o processamento");

    private final String description;
}