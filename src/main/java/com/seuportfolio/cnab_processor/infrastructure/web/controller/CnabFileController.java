package com.seuportfolio.cnab_processor.infrastructure.web.controller;

import com.seuportfolio.cnab_processor.application.service.CnabProcessingService;
import com.seuportfolio.cnab_processor.application.service.CnabQueryService;
import com.seuportfolio.cnab_processor.infrastructure.web.dto.CnabFileResponse;
import com.seuportfolio.cnab_processor.infrastructure.web.dto.TransactionResponse;
import com.seuportfolio.cnab_processor.infrastructure.web.dto.UploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/cnab")
@RequiredArgsConstructor
@Tag(name = "CNAB — Arquivos", description = "Upload e consulta de arquivos CNAB 240/400")
public class CnabFileController {

    private final CnabProcessingService processingService;
    private final CnabQueryService queryService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Faz upload e processa um arquivo CNAB",
            description = "Recebe um arquivo CNAB 240 ou 400, detecta o formato automaticamente e inicia o processamento via Spring Batch. Retorna o resultado do processamento de forma síncrona.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Arquivo processado com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Arquivo inválido ou formato não reconhecido"),
                    @ApiResponse(responseCode = "500", description = "Erro interno no processamento")
            }
    )
    public ResponseEntity<UploadResponse> upload(
            @Parameter(description = "Arquivo CNAB .rem ou .txt", required = true)
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Upload recebido: '{}' ({} bytes)", file.getOriginalFilename(), file.getSize());
        UploadResponse response = processingService.process(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/files")
    @Operation(summary = "Lista todos os arquivos CNAB processados (paginado)")
    public ResponseEntity<Page<CnabFileResponse>> listFiles(
            @PageableDefault(size = 20, sort = "receivedAt") Pageable pageable) {
        return ResponseEntity.ok(queryService.listFiles(pageable));
    }

    @GetMapping("/files/{id}")
    @Operation(summary = "Retorna os detalhes de um arquivo CNAB específico")
    public ResponseEntity<CnabFileResponse> getFile(
            @Parameter(description = "UUID do arquivo") @PathVariable UUID id) {
        return ResponseEntity.ok(queryService.getFile(id));
    }

    @GetMapping("/files/{id}/transactions")
    @Operation(summary = "Lista as transações de um arquivo CNAB (paginado)")
    public ResponseEntity<Page<TransactionResponse>> getTransactionsByFile(
            @Parameter(description = "UUID do arquivo") @PathVariable UUID id,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(queryService.getTransactionsByFile(id, pageable));
    }
}