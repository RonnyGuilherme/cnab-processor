package com.seuportfolio.cnab_processor.application.batch.processor;

import com.seuportfolio.cnab_processor.application.service.banco.BancoStrategyFactory;
import com.seuportfolio.cnab_processor.domain.model.TransactionRecord;
import com.seuportfolio.cnab_processor.domain.model.enums.TransactionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Valida e enriquece cada {@link TransactionRecord} antes da persistência.
 *
 * Regras aplicadas:
 * - Valor de pagamento deve ser maior que zero
 * - Data de pagamento não pode ser nula
 * - Se houver strategy do banco registrada, aplica enriquecimento específico
 *
 * Registros inválidos são marcados como {@link com.seuportfolio.cnab_processor.domain.model.enums.TransactionStatus#REJECTED}
 * e continuam no fluxo — são contabilizados e persistidos para auditoria.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CnabTransactionProcessor implements ItemProcessor<TransactionRecord, TransactionRecord> {

    private final BancoStrategyFactory strategyFactory;

    @Override
    public TransactionRecord process(TransactionRecord record) {
        log.debug("Processando registro linha {}", record.getLineNumber());

        if (record.getAmount() == null || record.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            record.reject("Valor de pagamento inválido ou zerado: " + record.getAmount());
            log.warn("Registro rejeitado (linha {}) — valor inválido: {}", record.getLineNumber(), record.getAmount());
            return record;
        }

        if (record.getPaymentDate() == null) {
            record.reject("Data de pagamento ausente.");
            log.warn("Registro rejeitado (linha {}) — data ausente", record.getLineNumber());
            return record;
        }

        if (record.getBeneficiaryName() == null || record.getBeneficiaryName().isBlank()) {
            record.reject("Nome do favorecido ausente.");
            log.warn("Registro rejeitado (linha {}) — favorecido ausente", record.getLineNumber());
            return record;
        }

        // Aplica enriquecimento específico do banco, se disponível
        if (record.getCnabFile() != null
                && strategyFactory.supports(record.getCnabFile().getBankCode())) {
            strategyFactory.getStrategy(record.getCnabFile().getBankCode()).enrich(record);
        }

        record.markAsProcessed();
        return record;
    }
}