package com.seuportfolio.cnab_processor.domain.model;

import com.seuportfolio.cnab_processor.domain.model.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pertence ao agregado {@link CnabFile} —
 * nunca é criado de forma independente.
 */
@Entity
@Table(name = "transaction_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cnab_file_id", nullable = false)
    private CnabFile cnabFile;

    @Column(nullable = false)
    private int lineNumber;

    @Column(nullable = false, length = 30)
    private String payerDocument;

    @Column(nullable = false, length = 60)
    private String beneficiaryName;

    @Column(nullable = false, length = 20)
    private String beneficiaryAgency;

    @Column(nullable = false, length = 20)
    private String beneficiaryAccount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column(length = 10)
    private String currencyType;

    @Column(length = 20)
    private String documentNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    /** Linha original preservada para auditoria. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawLine;

    @Builder
    public TransactionRecord(int lineNumber,
                             String payerDocument,
                             String beneficiaryName,
                             String beneficiaryAgency,
                             String beneficiaryAccount,
                             BigDecimal amount,
                             LocalDate paymentDate,
                             String currencyType,
                             String documentNumber,
                             String rawLine) {
        this.lineNumber       = lineNumber;
        this.payerDocument    = payerDocument;
        this.beneficiaryName  = beneficiaryName;
        this.beneficiaryAgency  = beneficiaryAgency;
        this.beneficiaryAccount = beneficiaryAccount;
        this.amount         = amount;
        this.paymentDate    = paymentDate;
        this.currencyType   = currencyType;
        this.documentNumber = documentNumber;
        this.rawLine        = rawLine;
        this.status         = TransactionStatus.PENDING;
        this.processedAt    = LocalDateTime.now();
    }

    // era: void associateTo(CnabFile file)
    public void associateTo(CnabFile file) {
        this.cnabFile = file;
    }

    public void markAsProcessed() {
        this.status      = TransactionStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        this.status          = TransactionStatus.REJECTED;
        this.rejectionReason = reason;
        this.processedAt     = LocalDateTime.now();
    }
}