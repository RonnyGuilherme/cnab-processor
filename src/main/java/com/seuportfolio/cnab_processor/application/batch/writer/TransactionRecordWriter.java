package com.seuportfolio.cnab_processor.application.batch.writer;

import com.seuportfolio.cnab_processor.domain.model.TransactionRecord;
import com.seuportfolio.cnab_processor.infrastructure.persistence.TransactionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Persiste cada chunk de {@link TransactionRecord} validados.
 *
 * Recebe apenas registros já processados pelo {@link CnabTransactionProcessor}
 * — tanto {@code PROCESSED} quanto {@code REJECTED} são persistidos para auditoria.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionRecordWriter implements ItemWriter<TransactionRecord> {

    private final TransactionRecordRepository repository;

    @Override
    public void write(Chunk<? extends TransactionRecord> chunk) {
        log.debug("Persistindo chunk de {} registros", chunk.size());
        repository.saveAll(chunk.getItems());
        log.debug("Chunk persistido com sucesso");
    }
}