package com.seuportfolio.cnab_processor.infrastructure.persistence;

import com.seuportfolio.cnab_processor.domain.model.CnabFile;
import com.seuportfolio.cnab_processor.domain.model.enums.BankCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CnabFileRepository extends JpaRepository<CnabFile, UUID> {

    List<CnabFile> findByBankCode(BankCode bankCode);

    List<CnabFile> findByReceivedAtBetween(LocalDateTime start, LocalDateTime end);
}
