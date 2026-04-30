# ── Estágio 1: Build ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copia apenas os arquivos de dependência primeiro (cache de layers)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q

# Copia o código e compila (pula testes — CI já rodou)
COPY src ./src
RUN ./mvnw package -DskipTests -q

# ── Estágio 2: Runtime ───────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Usuário não-root por segurança
RUN addgroup -S cnab && adduser -S cnab -G cnab
USER cnab

# Diretório para uploads temporários
RUN mkdir -p /tmp/cnab-processor

# Copia apenas o jar gerado
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]