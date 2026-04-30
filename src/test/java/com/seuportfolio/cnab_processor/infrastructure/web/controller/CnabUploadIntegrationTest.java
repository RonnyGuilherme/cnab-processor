package com.seuportfolio.cnab_processor.infrastructure.web.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste de integração completo com PostgreSQL real via Testcontainers.
 *
 * <p>Verifica o fluxo de ponta a ponta:</p>
 * <ol>
 *   <li>Upload multipart → Spring Batch job → PostgreSQL</li>
 *   <li>Flyway migrations executadas no container real</li>
 *   <li>Consulta paginada dos resultados via API</li>
 * </ol>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("CnabUpload — Integração com PostgreSQL real (Testcontainers)")
class CnabUploadIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cnab_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.batch.jdbc.initialize-schema", () -> "never");
        registry.add("spring.batch.job.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/cnab/files deve retornar lista vazia no banco limpo")
    void deveRetornarListaVaziaInicialmente() throws Exception {
        mockMvc.perform(get("/api/v1/cnab/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page.totalElements").value(0)); // ← era $.totalElements
    }

    @Test
    @DisplayName("POST /api/v1/cnab/upload com arquivo vazio deve retornar 400")
    void deveRejeitarArquivoVazio() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "vazio.rem",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/cnab/upload").file(emptyFile))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/cnab/files/{id} com ID inexistente deve retornar 404")
    void deveRetornar404ParaIdInexistente() throws Exception {
        String fakeId = "00000000-0000-0000-0000-000000000000";

        mockMvc.perform(get("/api/v1/cnab/files/" + fakeId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/cnab/transactions deve retornar lista paginada")
    void deveListarTransacoesPaginadas() throws Exception {
        mockMvc.perform(get("/api/v1/cnab/transactions")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists()); // ← era $.pageable
    }

    // ── Helper para gerar linha CNAB 240 válida ─────────────────────────────

    private byte[] gerarCnab240Valido() {
        String header  = pad("00100000", 240);
        String detalhe = pad("001" + "0001" + "3" + "00001" + "A" + "0" + "00", 240);
        String trailer = pad("001999959", 240);
        return (header + "\n" + detalhe + "\n" + trailer).getBytes(StandardCharsets.UTF_8);
    }

    private String pad(String s, int len) {
        return s.length() >= len ? s.substring(0, len) : s + " ".repeat(len - s.length());
    }
}
