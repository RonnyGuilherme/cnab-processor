# CNAB Processor

API REST para processamento de arquivos bancários **CNAB 240** e **CNAB 400** (padrão FEBRABAN).  
Projeto de portfólio demonstrando Clean Architecture, Spring Batch, Testcontainers e observabilidade com Prometheus/Grafana.

![CI](https://github.com/RonnyGuilherme/cnab-processor/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.5 + Spring Batch 5 |
| Persistência | PostgreSQL 16 + Flyway + Spring Data JPA |
| API | Spring MVC REST + SpringDoc OpenAPI (Swagger) |
| Observabilidade | Micrometer + Prometheus + Grafana |
| Testes | JUnit 5 + Mockito + Testcontainers |
| Containerização | Docker multi-stage + Docker Compose |
| CI | GitHub Actions |

---

## Funcionalidades

- Upload de arquivos CNAB 240 e 400 com detecção automática de formato
- Validação de dígito verificador Módulo 10 e Módulo 11 (variante FEBRABAN e BB)
- Enriquecimento de dados por banco (BB, Itaú, Bradesco) via Strategy Pattern
- Processamento em chunks via Spring Batch com resiliência a falhas parciais
- API REST paginada com filtro por status de transação
- Métricas de processamento expostas via `/actuator/prometheus`
- Documentação interativa via Swagger UI em `/swagger-ui.html`

---

## Rodando localmente

### Pré-requisitos

- Docker e Docker Compose instalados
- Porta 8080, 5432, 9090 e 3000 livres

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
POST   /api/v1/cnab/upload                    Upload e processamento de arquivo CNAB
GET    /api/v1/cnab/files                     Lista arquivos processados (paginado)
GET    /api/v1/cnab/files/{id}                Detalhes de um arquivo
GET    /api/v1/cnab/files/{id}/transactions   Transações de um arquivo (paginado)
GET    /api/v1/cnab/transactions?status=      Lista transações com filtro por status
```

---

## Arquitetura

```
src/main/java/com/seuportfolio/cnab_processor/
├── domain/               # Modelos, enums, interfaces de domínio
├── application/          # Casos de uso, parsers, strategies, batch
└── infrastructure/       # Controllers, repositórios, config, exceções
```

Decisões arquiteturais relevantes:

- **Strategy Pattern** para parsers CNAB e estratégias por banco — extensível via `Map<Enum, Interface>` injetado pelo Spring (Open/Closed Principle)
- **Spring Batch** com `@StepScope` reader, processor e writer separados — permite reprocessamento e skip de registros inválidos sem abortar o job
- **Clean Architecture** — domínio sem dependências de framework; infraestrutura depende do domínio, nunca o contrário
- **Testcontainers** — testes de integração contra PostgreSQL real, sem mocks de banco

---

## Roadmap (Fase 6)

- [ ] Suporte a segmentos B, C, J do CNAB 240 (boletos e tributos)
- [ ] Layouts por banco externalizados em YAML (`src/main/resources/layouts/`)
- [ ] `FlatFileItemReader` nativo para arquivos grandes (streaming em vez de `readAllLines`)
- [ ] Reabilitar JaCoCo com Java 21 (desabilitado temporariamente por incompatibilidade com Java 26)