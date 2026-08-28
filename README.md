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

Then open [the gateway Swagger UI](http://localhost:18080/swagger-ui/index.html). It provides a selector for each
service. Direct service UIs are also available at `http://localhost:18081` through `18084` under `/swagger-ui/index.html`.

## Local Docker ports

Hotel Booking uses a project-specific host-port range to reduce collisions with other local projects. Docker-internal
addresses remain unchanged; for example, services still connect to `postgres:5432` and `kafka:9092`.

| Component | Default host port | Override variable |
| --- | ---: | --- |
| Grafana | 13001 | `HB_GRAFANA_PORT` |
| Public website | 13002 | `HB_PUBLIC_WEB_PORT` |
| Admin website | 13004 | `HB_ADMIN_WEB_PORT` |
| PostgreSQL | 15432 | `HB_POSTGRES_PORT` |
| API gateway | 18080 | `HB_GATEWAY_PORT` |
| Hotel service | 18081 | `HB_HOTEL_SERVICE_PORT` |
| Booking service | 18082 | `HB_BOOKING_SERVICE_PORT` |
| Identity service | 18083 | `HB_IDENTITY_SERVICE_PORT` |
| Notification service | 18084 | `HB_NOTIFICATION_SERVICE_PORT` |
| Mailpit web UI | 18025 | `HB_MAILPIT_UI_PORT` |
| Mailpit SMTP | 11025 | `HB_MAILPIT_SMTP_PORT` |
| Prometheus | 19090 | `HB_PROMETHEUS_PORT` |
| Kafka | 19092 | `HB_KAFKA_PORT` |

Override a port in `.env` when necessary, for example `HB_POSTGRES_PORT=25432`. The `.env` file is ignored by Git.

## Local JWT signing keys

The identity service signs access tokens with an RSA private key; the other services only receive the matching public
key. Generate a disposable local pair before starting Docker Compose. In Git Bash, from the repository root:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
export JWT_PRIVATE_KEY_BASE64="$(openssl pkcs8 -topk8 -nocrypt -in jwt-private.pem -outform DER | openssl base64 -A)"
export JWT_PUBLIC_KEY_BASE64="$(openssl pkey -in jwt-private.pem -pubout -outform DER | openssl base64 -A)"
rm jwt-private.pem
docker compose up --build
```

The variables live only in that shell session. Never commit the private key or either environment variable. The CI
workflow generates a separate ephemeral pair for every run. Staging obtains the real values from AWS Systems Manager
Parameter Store: the private key is a `SecureString` and the public key is a `String`.

To generate and store a new staging pair without printing either key, run this from Git Bash after `aws login`:

```bash
bash infrastructure/aws/staging/create-jwt-parameters.sh
```

This replaces both staging keys. Deploy immediately afterwards; existing access tokens will stop validating, which is
the expected effect of key rotation.
