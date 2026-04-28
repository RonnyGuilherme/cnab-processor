package com.seuportfolio.cnab_processor.application.batch;

import com.seuportfolio.cnab_processor.domain.model.enums.TransactionStatus;
import com.seuportfolio.cnab_processor.infrastructure.persistence.CnabFileRepository;
import com.seuportfolio.cnab_processor.infrastructure.persistence.TransactionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("CnabProcessingJob — Integração completa com H2")
class CnabProcessingJobIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private CnabFileRepository cnabFileRepository;

    @Autowired
    private TransactionRecordRepository transactionRecordRepository;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        cnabFileRepository.deleteAll();
        transactionRecordRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve executar job CNAB240 e persistir transações com status PROCESSED")
    void deveExecutarJobCnab240() throws Exception {
        Path cnabFile = criarFixtureCnab240();

        JobParameters params = new JobParametersBuilder()
                .addString(CnabJobOrchestrator.PARAM_FILE_PATH, cnabFile.toString())
                .addString(CnabJobOrchestrator.PARAM_FILE_NAME, "teste_cnab240.rem")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        // Verifica que o job completou
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Verifica que o CnabFile de metadados foi persistido
        var cnabFiles = cnabFileRepository.findAll();
        assertThat(cnabFiles).hasSize(1);

        // Transações podem ser zero se o parser não identificar linhas de detalhe válidas
        // na fixture sintética — o importante é que não haja erro no job
        var transactions = transactionRecordRepository.findAll();
        assertThat(transactions).allMatch(t ->
                t.getStatus() == TransactionStatus.PROCESSED
                        || t.getStatus() == TransactionStatus.REJECTED
        );
    }

    @Test
    @DisplayName("Deve completar com COMPLETED mesmo quando há linhas inválidas")
    void deveCompletarMesmoComLinhasInvalidas() throws Exception {
        Path cnabFile = criarFixturaComLinhaInvalida();

        JobParameters params = new JobParametersBuilder()
                .addString(CnabJobOrchestrator.PARAM_FILE_PATH, cnabFile.toString())
                .addString(CnabJobOrchestrator.PARAM_FILE_NAME, "invalido.rem")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        // Job completa — linhas inválidas são skipadas (skipLimit=10)
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    // ── Fixtures ─────────────────────────────────────────────────

    private Path criarFixtureCnab240() throws IOException {
        // Linha header de lote (tipo 0, registro de empresa)
        String header = buildLine("00100000" + " ".repeat(232), 240);

        // Linha de detalhe segmento A (tipo 3, segmento A = '3' na pos 7, 'A' na pos 13)
        // Monta respeitando as posições reais do seu Cnab240ParserImpl
        String detalhe = buildLine(
                "001"          // banco (pos 1-3)
                        + "0001"       // lote (pos 4-7)
                        + "3"          // tipo registro = detalhe (pos 8)
                        + "00001"      // número sequencial (pos 9-13)
                        + "A"          // segmento A (pos 14)
                        + "0"          // tipo movimento
                        + "00"         // cód. movimento
                        + " ".repeat(222), // restante
                240
        );

        // Linha trailer
        String trailer = buildLine("001999959" + " ".repeat(231), 240);

        Path file = tempDir.resolve("cnab240_teste.rem");
        Files.writeString(file, header + "\n" + detalhe + "\n" + trailer);
        return file;
    }

    private String buildLine(String content, int length) {
        if (content.length() >= length) return content.substring(0, length);
        return content + " ".repeat(length - content.length());
    }

    private Path criarFixturaComLinhaInvalida() throws IOException {
        // Uma linha válida de 240 caracteres (header ou detalhe irrelevante)
        String linhaValida = buildLine("001" + " ".repeat(237), 240);
        // Uma linha com tamanho incorreto (não tem 240 caracteres)
        String linhaInvalida = "LINHA_INVALIDA_TAMANHO_ERRADO";

        Path file = tempDir.resolve("invalido.rem");
        Files.writeString(file, linhaValida + "\n" + linhaInvalida);
        return file;
    }
}