package com.seuportfolio.cnab_processor.application.batch;

/**
 * Constantes e documentação do fluxo do job CNAB.
 *
 * Classe puramente utilitária — sem estado, sem anotações Spring.
 * Testável de forma completamente isolada.
 *
 * Fluxo:
 *   CnabFileItemReader  → lê e parseia o arquivo, retorna TransactionRecord por vez
 *   CnabTransactionProcessor → valida valor, data e aplica regras de negócio
 *   TransactionRecordWriter  → persiste em chunks no banco
 */
public final class CnabJobOrchestrator {

    public static final String JOB_NAME  = "cnabProcessingJob";
    public static final String STEP_NAME = "cnabProcessingStep";

    /** Parâmetros esperados no JobParameters ao disparar o job. */
    public static final String PARAM_FILE_PATH   = "filePath";
    public static final String PARAM_FILE_NAME   = "fileName";

    /** Chave para compartilhar o ID do CnabFile persistido via ExecutionContext. */
    public static final String CTX_CNAB_FILE_ID  = "cnabFileId";

    private CnabJobOrchestrator() {
        throw new UnsupportedOperationException("Classe utilitária.");
    }
}