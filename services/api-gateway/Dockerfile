FROM maven:3.9.16-eclipse-temurin-21 AS build

WORKDIR /build

COPY . .

RUN chmod +x mvnw && ./mvnw -pl services/api-gateway -am package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /build/services/api-gateway/target/api-gateway-*-exec.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD curl --fail http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
