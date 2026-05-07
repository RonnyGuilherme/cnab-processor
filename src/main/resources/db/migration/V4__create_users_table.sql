CREATE TABLE users (
                       id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       username   VARCHAR(50)  NOT NULL UNIQUE,
                       password   VARCHAR(255) NOT NULL,
                       role       VARCHAR(20)  NOT NULL DEFAULT 'USER',
                       enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Usuários padrão (senhas serão sobrescritas pelo DataInitializer com BCrypt)
-- As senhas abaixo são placeholders — DataInitializer insere com encode correto
-- Sem INSERTs — DataInitializer gerencia os usuários padrão com senha BCrypt
