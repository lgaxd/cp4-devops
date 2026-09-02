# =========================================================
# Dockerfile.app - Imagem da API SpaceCrop (Java 21 / Spring Boot)
# =========================================================

# ---- Etapa 1: build com Maven ----
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# ---- Etapa 2: imagem final, enxuta e sem privilégios de root ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Cria usuário e grupo dedicados (não root)
RUN addgroup -S lga && adduser -S lga -G lga

COPY --from=builder /build/target/*.jar app.jar
RUN chown lga:lga app.jar

# Executa como usuário sem privilégios administrativos
USER lga
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
