FROM maven:3.9.16-eclipse-temurin-21 AS build

WORKDIR /build
COPY . .
RUN chmod +x mvnw && ./mvnw -pl services/hotel-service -am package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /build/services/hotel-service/target/hotel-service-*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
