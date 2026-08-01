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
