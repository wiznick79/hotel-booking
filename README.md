# Hotel Booking System

A small hotel booking system built as a practical microservices learning project.

## Goals

- Support guest bookings without requiring an account.
- Support optional customer accounts and booking history.
- Provide staff-focused hotel and reservation management.
- Learn microservices, messaging, containers, security, testing, and AWS deployment.

## Repository structure

- `services/` — independently runnable Spring Boot services.
- `docs/` — architecture, domain, and decision documentation.
- `infrastructure/` — local Docker and later AWS infrastructure definitions.

## Technology baseline

- Java 21
- Spring Boot
- Maven Wrapper
- PostgreSQL
- Docker
- AWS

## Local API documentation

Swagger/OpenAPI is disabled by default and must never be enabled in production.

For an IDE run, add this to your ignored local profile:

```properties
app.use-swagger=true
```

For Docker Compose, start the stack with:

```powershell
$env:USE_SWAGGER='true'
docker compose up --build
```

Then open [the gateway Swagger UI](http://localhost:8080/swagger-ui/index.html). It provides a selector for each
service. Direct service UIs are also available at `http://localhost:8081` through `8084` under `/swagger-ui/index.html`.
