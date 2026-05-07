package com.seuportfolio.cnab_processor.infrastructure.config;

import com.seuportfolio.cnab_processor.application.batch.CnabJobOrchestrator;
import com.seuportfolio.cnab_processor.application.batch.processor.CnabTransactionProcessor;
import com.seuportfolio.cnab_processor.application.batch.reader.CnabFileItemReader;
import com.seuportfolio.cnab_processor.application.batch.writer.TransactionRecordWriter;
import com.seuportfolio.cnab_processor.domain.model.TransactionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuração do job Spring Batch para processamento de arquivos CNAB.
 * Declarada como {@code record} com {@code proxyBeanMethods = false}:
 * todas as dependências chegam via construtor — sem chamadas cruzadas
 * entre métodos {@code @Bean}, o proxy CGLIB é desnecessário.
 * O {@link CnabFileItemReader} é registrado explicitamente como listener
 * do step para que seus callbacks {@code @BeforeStep} e {@code @AfterStep}
 * sejam invocados pelo Spring Batch.
 */
@Configuration(proxyBeanMethods = false)
public record BatchConfiguration(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        CnabFileItemReader cnabFileItemReader,
        CnabTransactionProcessor cnabTransactionProcessor,
        TransactionRecordWriter transactionRecordWriter,
        @Value("${cnab.batch.chunk-size:100}") int chunkSize
) {

    private static final Logger log = LoggerFactory.getLogger(BatchConfiguration.class);

    @Bean
    public Step cnabProcessingStep() {
        log.info("Construindo step [{}] — chunkSize={}", CnabJobOrchestrator.STEP_NAME, chunkSize);

        return new StepBuilder(CnabJobOrchestrator.STEP_NAME, jobRepository)
                .<TransactionRecord, TransactionRecord>chunk(chunkSize, transactionManager)
                .reader(cnabFileItemReader)
                .processor(cnabTransactionProcessor)
                .writer(transactionRecordWriter)
                .listener(cnabFileItemReader)   // registra @BeforeStep / @AfterStep
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(10)
                .build();
    }

    @Bean
    public Job cnabProcessingJob(Step cnabProcessingStep) {
        log.info("Construindo job [{}]", CnabJobOrchestrator.JOB_NAME);

        return new JobBuilder(CnabJobOrchestrator.JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(cnabProcessingStep)
                .build();



    }
    /**
     * Job launcher assíncrono — lança o job em thread separada e retorna
     * imediatamente o {@link JobExecution} com status STARTING.
     * O cliente consulta o status via {@code GET /api/v1/jobs/{executionId}}.
     */
    @Bean
    public JobLauncher asyncJobLauncher(JobRepository jobRepository) {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(new SimpleAsyncTaskExecutor("cnab-batch-"));
        return launcher;
    }

    /**
     * Job launcher síncrono — mantido para uso nos testes (@SpringBatchTest).
     */
    @Bean
    @Primary
    public JobLauncher syncJobLauncher(JobRepository jobRepository) {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(new SyncTaskExecutor());
        return launcher;
    }
}