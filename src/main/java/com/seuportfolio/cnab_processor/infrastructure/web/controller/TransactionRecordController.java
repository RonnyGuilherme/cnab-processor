package com.seuportfolio.cnab_processor.infrastructure.web.controller;

import com.seuportfolio.cnab_processor.application.service.CnabQueryService;
import com.seuportfolio.cnab_processor.domain.model.enums.TransactionStatus;
import com.seuportfolio.cnab_processor.infrastructure.web.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cnab")
@RequiredArgsConstructor
@Tag(name = "CNAB — Transações", description = "Consulta de transações processadas")
public class TransactionRecordController {

    private final CnabQueryService queryService;

    @GetMapping("/transactions")
    @Operation(summary = "Lista todas as transações com filtro opcional por status")
    public ResponseEntity<Page<TransactionResponse>> listTransactions(
            @Parameter(description = "Filtro por status: PENDING, PROCESSED, REJECTED")
            @RequestParam(required = false) TransactionStatus status,
            @PageableDefault(size = 50, sort = "paymentDate") Pageable pageable) {
        return ResponseEntity.ok(queryService.listTransactions(status, pageable));
    }
}