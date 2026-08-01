FROM maven:3.9.16-eclipse-temurin-21 AS build

WORKDIR /build
COPY . .
RUN chmod +x mvnw && ./mvnw -pl services/notification-service -am package -DskipTests

FROM eclipse-temurin:25-jre

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /build/services/notification-service/target/notification-service-*-exec.jar app.jar

EXPOSE 8084
HEALTHCHECK --interval=10s --timeout=3s --retries=10 CMD curl --fail --silent http://localhost:8084/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
