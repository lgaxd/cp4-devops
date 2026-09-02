# =========================================================
# SpaceCrop API - Java 21 / Spring Boot
# =========================================================

FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S lga \
    && adduser -S lga -G lga

COPY --from=builder /build/target/*.jar app.jar
RUN chown lga:lga app.jar

USER lga

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
