CREATE TABLE cnab_files (
                            id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
                            original_file_name  VARCHAR(255)  NOT NULL,
                            cnab_type           VARCHAR(10)   NOT NULL CHECK (cnab_type IN ('CNAB240', 'CNAB400')),
                            bank_code           VARCHAR(20)   NOT NULL,
                            received_at         TIMESTAMP     NOT NULL,
                            updated_at          TIMESTAMP     NOT NULL,
                            total_lines         INT           NOT NULL DEFAULT 0,
                            processed_lines     INT           NOT NULL DEFAULT 0,
                            rejected_lines      INT           NOT NULL DEFAULT 0
);

CREATE TABLE transaction_records (
                                     id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
                                     cnab_file_id        UUID          NOT NULL REFERENCES cnab_files(id) ON DELETE CASCADE,
                                     line_number         INT           NOT NULL,
                                     payer_document      VARCHAR(30)   NOT NULL,
                                     beneficiary_name    VARCHAR(60)   NOT NULL,
                                     beneficiary_agency  VARCHAR(20)   NOT NULL,
                                     beneficiary_account VARCHAR(20)   NOT NULL,
                                     amount              NUMERIC(15,2) NOT NULL,
                                     payment_date        DATE          NOT NULL,
                                     currency_type       VARCHAR(10),
                                     document_number     VARCHAR(20),
                                     status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
                                     rejection_reason    TEXT,
                                     processed_at        TIMESTAMP     NOT NULL,
                                     raw_line            TEXT          NOT NULL
);

CREATE INDEX idx_transaction_cnab_file    ON transaction_records(cnab_file_id);
CREATE INDEX idx_transaction_status       ON transaction_records(status);
CREATE INDEX idx_transaction_payment_date ON transaction_records(payment_date);
CREATE INDEX idx_cnab_files_bank_code     ON cnab_files(bank_code);
CREATE INDEX idx_cnab_files_received_at   ON cnab_files(received_at DESC);