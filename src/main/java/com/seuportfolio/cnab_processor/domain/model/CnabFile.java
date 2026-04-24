package com.seuportfolio.cnab_processor.domain.model;

import com.seuportfolio.cnab_processor.domain.model.enums.BankCode;
import com.seuportfolio.cnab_processor.domain.model.enums.CnabType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Agregado raiz: centraliza o ciclo de vida do arquivo CNAB
 * e das transações que ele contém.
 */
@Entity
@Table(name = "cnab_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = "transactions")
public class CnabFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String originalFileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CnabType cnabType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BankCode bankCode;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private int totalLines;

    @Column(nullable = false)
    private int processedLines;

    @Column(nullable = false)
    private int rejectedLines;

    @OneToMany(mappedBy = "cnabFile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionRecord> transactions = new ArrayList<>();

    private CnabFile(String originalFileName, CnabType cnabType, BankCode bankCode) {
        this.originalFileName = originalFileName;
        this.cnabType = cnabType;
        this.bankCode = bankCode;
        this.receivedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /** Factory method — garante invariantes na criação. */
    public static CnabFile receive(String originalFileName, CnabType cnabType, BankCode bankCode) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("Nome do arquivo não pode ser vazio.");
        }
        return new CnabFile(originalFileName, cnabType, bankCode);
    }

    public void addTransaction(TransactionRecord transaction) {
        transactions.add(transaction);
        transaction.associateTo(this);
        this.updatedAt = LocalDateTime.now();
    }

    public void registerProcessingResult(int processedLines, int rejectedLines) {
        this.processedLines = processedLines;
        this.rejectedLines = rejectedLines;
        this.totalLines = processedLines + rejectedLines;
        this.updatedAt = LocalDateTime.now();
    }

    /** Lista imutável — protege o encapsulamento do agregado. */
    public List<TransactionRecord> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }
}