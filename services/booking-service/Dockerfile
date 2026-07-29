FROM maven:3.9.16-eclipse-temurin-21 AS build

WORKDIR /build
COPY . .
RUN chmod +x mvnw && ./mvnw -pl services/booking-service -am package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /build/services/booking-service/target/booking-service-*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
