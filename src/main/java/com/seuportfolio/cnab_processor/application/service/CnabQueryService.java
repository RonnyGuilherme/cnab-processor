package com.seuportfolio.cnab_processor.application.service;

import com.seuportfolio.cnab_processor.domain.model.CnabFile;
import com.seuportfolio.cnab_processor.domain.model.TransactionRecord;
import com.seuportfolio.cnab_processor.domain.model.enums.TransactionStatus;
import com.seuportfolio.cnab_processor.infrastructure.persistence.CnabFileRepository;
import com.seuportfolio.cnab_processor.infrastructure.persistence.TransactionRecordRepository;
import com.seuportfolio.cnab_processor.infrastructure.web.dto.CnabFileResponse;
import com.seuportfolio.cnab_processor.infrastructure.web.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CnabQueryService {

    private final CnabFileRepository cnabFileRepository;
    private final TransactionRecordRepository transactionRecordRepository;

    public Page<CnabFileResponse> listFiles(Pageable pageable) {
        return cnabFileRepository.findAll(pageable).map(CnabFileResponse::from);
    }

    public CnabFileResponse getFile(UUID id) {
        return cnabFileRepository.findById(id)
                .map(CnabFileResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Arquivo CNAB não encontrado: " + id));
    }

    public Page<TransactionResponse> getTransactionsByFile(UUID fileId, Pageable pageable) {
        if (!cnabFileRepository.existsById(fileId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Arquivo CNAB não encontrado: " + fileId);
        }
        return transactionRecordRepository
                .findByCnabFileId(fileId, pageable)
                .map(TransactionResponse::from);
    }

    public Page<TransactionResponse> listTransactions(TransactionStatus status, Pageable pageable) {
        Page<TransactionRecord> page = (status != null)
                ? transactionRecordRepository.findByStatus(status, pageable)
                : transactionRecordRepository.findAll(pageable);

        return page.map(TransactionResponse::from);
    }
}