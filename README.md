# CNAB Processor

API REST para processamento de arquivos bancários **CNAB 240** e **CNAB 400** (padrão FEBRABAN).  
Projeto de portfólio demonstrando Clean Architecture, Spring Batch, Spring Security com JWT,
Testcontainers e observabilidade com Prometheus/Grafana.

![CI](https://github.com/RonnyGuilherme/cnab-processor/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)
![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-blue)
![Tests](https://img.shields.io/badge/testes-65%20passando-brightgreen)

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.5 + Spring Batch 5 |
| Segurança | Spring Security + JWT (jjwt 0.12) |
| Persistência | PostgreSQL 16 + Flyway + Spring Data JPA |
| API | Spring MVC REST + SpringDoc OpenAPI (Swagger) |
| Observabilidade | Micrometer + Prometheus + Grafana |
| Testes | JUnit 5 + Mockito + Testcontainers |
| Containerização | Docker multi-stage + Docker Compose |
| CI | GitHub Actions |

---

## Funcionalidades

- Upload de arquivos CNAB 240 e 400 com detecção automática de formato
- Suporte a segmentos A (transferência), B (endereço do favorecido) e J (pagamento de boleto) do CNAB 240
- Layouts de posições de campos externalizados em YAML por banco — sem recompilar para adaptar a novos bancos
- Leitura em streaming via `BufferedReader` — suporta arquivos grandes sem risco de `OutOfMemoryError`
- Idempotência no upload por hash SHA-256 — reprocessamento do mesmo arquivo retorna o resultado original
- Validação de dígito verificador Módulo 10 e Módulo 11 (variante FEBRABAN e BB)
- Enriquecimento de dados por banco (BB, Itaú, Bradesco) via Strategy Pattern
- Processamento em chunks via Spring Batch com resiliência a falhas parciais
- Autenticação JWT — endpoints protegidos, `/auth/login` público
- API REST paginada com filtro por status de transação
- Métricas de processamento expostas via `/actuator/prometheus`
- Documentação interativa via Swagger UI em `/swagger-ui.html`

---

## Autenticação

A API usa JWT. Obtenha um token antes de chamar os endpoints protegidos:

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"api_user","password":"user123"}'
```

Use o token retornado no header `Authorization: Bearer <token>` nas chamadas subsequentes.

| Usuário | Senha | Role | Acesso |
|---|---|---|---|
| `api_user` | `user123` | USER | Endpoints `/api/v1/**` |
| `admin` | `admin123` | ADMIN | Endpoints `/api/v1/**` + `/actuator/**` |

---

## Rodando localmente

### Pré-requisitos

- Docker e Docker Compose instalados
- Portas 8080, 5432, 9090 e 3000 livres

### Subir tudo com Docker Compose

```bash
docker compose up --build
```

| Serviço | URL |
|---|---|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Actuator Health | http://localhost:8080/actuator/health |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin / admin) |

### Rodar apenas os testes

```bash
./mvnw test
```

> Requer Docker rodando — os testes de integração usam Testcontainers (PostgreSQL 16-alpine).

---

## Endpoints principais

```
POST   /auth/login                            Autenticação e geração de token JWT

POST   /api/v1/cnab/upload                   Upload e processamento de arquivo CNAB
GET    /api/v1/cnab/files                     Lista arquivos processados (paginado)
GET    /api/v1/cnab/files/{id}               Detalhes de um arquivo
GET    /api/v1/cnab/files/{id}/transactions  Transações de um arquivo (paginado)
GET    /api/v1/cnab/transactions?status=     Lista transações com filtro por status
```

---

## Arquitetura

```
src/main/java/com/seuportfolio/cnab_processor/
├── domain/               # Modelos, enums, interfaces de domínio
├── application/          # Casos de uso, parsers, strategies, batch
└── infrastructure/       # Controllers, repositórios, config, exceções, security
```

```
src/main/resources/
├── layouts/              # Posições de campos CNAB por banco (YAML)
│   ├── cnab240-default.yml
│   ├── cnab240-001.yml   # Banco do Brasil
│   └── cnab400-default.yml
└── db/migration/         # Scripts Flyway (V1, V2, V3)
```

Decisões arquiteturais relevantes:

- **Strategy Pattern** para parsers CNAB e estratégias por banco — extensível via `Map<Enum, Interface>` injetado pelo Spring (Open/Closed Principle)
- **Spring Batch** com `@StepScope` reader, processor e writer separados — permite reprocessamento e skip de registros inválidos sem abortar o job
- **Streaming com lookahead** — `BufferedReader` lê linha a linha; segmento B enriquece o registro A anterior antes de emiti-lo ao processor
- **Layouts externalizados** — posições de campos em YAML com herança (banco específico sobrescreve o default), sem recompilação para adaptar novos bancos
- **Idempotência por hash** — SHA-256 do conteúdo do arquivo garante que reenvios retornem o resultado original sem reprocessamento
- **Clean Architecture** — domínio sem dependências de framework; infraestrutura depende do domínio, nunca o contrário
- **Testcontainers** — testes de integração contra PostgreSQL real, sem mocks de banco

---
