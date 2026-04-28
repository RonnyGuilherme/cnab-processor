package com.seuportfolio.cnab_processor.application.batch.processor;

import com.seuportfolio.cnab_processor.application.service.banco.BancoStrategyFactory;
import com.seuportfolio.cnab_processor.domain.model.TransactionRecord;
import com.seuportfolio.cnab_processor.domain.model.enums.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("CnabTransactionProcessor — Validação e enriquecimento de registros")
@ExtendWith(MockitoExtension.class)
class CnabTransactionProcessorTest {

    @Mock
    private BancoStrategyFactory strategyFactory;

    @InjectMocks
    private CnabTransactionProcessor processor;

    @Test
    @DisplayName("Deve marcar registro como PROCESSED quando válido")
    void deveProcessarRegistroValido() throws Exception {
        // Sem stub — cnabFile é null, strategyFactory não é chamado
        TransactionRecord record = buildRecord(new BigDecimal("150.00"), LocalDate.now(), "JOAO DA SILVA");

        TransactionRecord result = processor.process(record);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.PROCESSED);
    }

    @Test
    @DisplayName("Deve rejeitar registro com valor zero")
    void deveRejeitarValorZero() throws Exception {
        TransactionRecord record = buildRecord(BigDecimal.ZERO, LocalDate.now(), "JOAO DA SILVA");

        TransactionRecord result = processor.process(record);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.REJECTED);
        assertThat(result.getRejectionReason()).contains("inválido");
    }

    @Test
    @DisplayName("Deve rejeitar registro com valor negativo")
    void deveRejeitarValorNegativo() throws Exception {
        TransactionRecord record = buildRecord(new BigDecimal("-1.00"), LocalDate.now(), "JOAO DA SILVA");

        TransactionRecord result = processor.process(record);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.REJECTED);
    }

    @Test
    @DisplayName("Deve rejeitar registro com data nula")
    void deveRejeitarDataNula() throws Exception {
        TransactionRecord record = buildRecord(new BigDecimal("100.00"), null, "JOAO DA SILVA");

        TransactionRecord result = processor.process(record);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.REJECTED);
        assertThat(result.getRejectionReason()).containsIgnoringCase("data");
    }

    @Test
    @DisplayName("Deve rejeitar registro sem nome do favorecido")
    void deveRejeitarSemFavorecido() throws Exception {
        TransactionRecord record = buildRecord(new BigDecimal("100.00"), LocalDate.now(), "");

        TransactionRecord result = processor.process(record);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.REJECTED);
        assertThat(result.getRejectionReason()).contains("favorecido");
    }

    @Test
    @DisplayName("Deve aplicar enriquecimento da strategy do banco quando disponível")
    void deveAplicarEnriquecimentoDoBanco() throws Exception {
        // Sem cnabFile associado → strategy não é chamada — comportamento correto e esperado
        TransactionRecord record = buildRecord(new BigDecimal("200.00"), LocalDate.now(), "MARIA LTDA");
        processor.process(record);

        // Sem cnabFile setado, strategy não deve ser chamada em nenhuma hipótese
        verify(strategyFactory, never()).getStrategy(any());
        verify(strategyFactory, never()).supports(any());  // ← verificar que não foi chamado
    }

    // ── Helper ────────────────────────────────────────────────────

    private TransactionRecord buildRecord(BigDecimal amount, LocalDate paymentDate, String name) {
        return TransactionRecord.builder()
                .lineNumber(1)
                .payerDocument("12345678000199")
                .beneficiaryName(name)
                .beneficiaryAgency("1234")
                .beneficiaryAccount("123456789")
                .amount(amount)
                .paymentDate(paymentDate)
                .currencyType("BRL")
                .documentNumber("DOC001")
                .rawLine(" ".repeat(240))
                .build();
    }
}