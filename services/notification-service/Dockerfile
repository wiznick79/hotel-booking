FROM maven:3.9.16-eclipse-temurin-21 AS build

WORKDIR /build
COPY . .
RUN chmod +x mvnw && ./mvnw -pl services/notification-service -am package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /build/services/notification-service/target/notification-service-*.jar app.jar

EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
