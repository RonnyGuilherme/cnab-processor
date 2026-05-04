-- Idempotência: hash SHA-256 do arquivo
ALTER TABLE cnab_files
    ADD COLUMN IF NOT EXISTS file_hash VARCHAR(64),
    ADD CONSTRAINT uk_cnab_files_hash UNIQUE (file_hash);

-- Campos para segmentos B e J do CNAB 240
ALTER TABLE transaction_records
    ADD COLUMN IF NOT EXISTS segment_type        CHAR(1),
    ADD COLUMN IF NOT EXISTS bar_code            VARCHAR(44),
    ADD COLUMN IF NOT EXISTS assignor_name       VARCHAR(30),
    ADD COLUMN IF NOT EXISTS beneficiary_address VARCHAR(30),
    ADD COLUMN IF NOT EXISTS beneficiary_city    VARCHAR(20),
    ADD COLUMN IF NOT EXISTS beneficiary_state   CHAR(2),
    ADD COLUMN IF NOT EXISTS beneficiary_cep     VARCHAR(8);