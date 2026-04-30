package com.seuportfolio.cnab_processor.infrastructure.persistence;

import com.seuportfolio.cnab_processor.domain.model.TransactionRecord;
import com.seuportfolio.cnab_processor.domain.model.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, UUID> {

    Page<TransactionRecord> findByCnabFileId(UUID cnabFileId, Pageable pageable);

    List<TransactionRecord> findByCnabFileIdAndStatus(UUID cnabFileId, TransactionStatus status);

    long countByCnabFileIdAndStatus(UUID cnabFileId, TransactionStatus status);

    List<TransactionRecord> findByPaymentDateBetween(LocalDate start, LocalDate end);

    Page<TransactionRecord> findByStatus(TransactionStatus status, Pageable pageable);

}