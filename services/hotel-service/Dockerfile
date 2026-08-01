FROM maven:3-eclipse-temurin-26 AS build

WORKDIR /build
COPY . .
RUN chmod +x mvnw && ./mvnw -pl services/hotel-service -am package -DskipTests

FROM eclipse-temurin:21-jre

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /build/services/hotel-service/target/hotel-service-*-exec.jar app.jar

EXPOSE 8081
HEALTHCHECK --interval=10s --timeout=3s --retries=10 CMD curl --fail --silent http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
