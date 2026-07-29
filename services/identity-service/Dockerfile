FROM maven:3.9.16-eclipse-temurin-21 AS build

WORKDIR /build
COPY . .
RUN chmod +x mvnw && ./mvnw -pl services/identity-service -am package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /build/services/identity-service/target/identity-service-*.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
