# Service resilience

## Protected boundaries

The system currently has five synchronous resilience boundaries:

- API gateway to every downstream service;
- booking-service to hotel-service for catalogue, pricing, room, and availability data;
- notification-service to hotel-service for hotel-specific contact and sender data;
- booking-service to Stripe for hosted Checkout creation;
- notification-service to SMTP for email delivery.

Kafka delivery already has separate retry and dead-letter behaviour and is not wrapped in these HTTP circuit
breakers.

Stripe Checkout creation has a 2-second connection timeout, a 5-second read timeout, and a stable payment-attempt
idempotency key. It is not retried synchronously: a response can be lost after Stripe accepts a request, so an
automatic retry could duplicate provider work. SMTP uses the JavaMail connection/read/write timeouts and the existing
persisted notification retry schedule.

## Failure policy

HTTP clients use a 2-second connection timeout and a 5-second read/response timeout by default. These values can
be overridden with environment variables.

Booking-service and notification-service retry a transient hotel-service failure once after 150 milliseconds.
Only transport failures and `5xx` responses are retryable. A validation or other `4xx` response is a completed
business response and is never retried.

The gateway retries one time only for `GET` requests that end in a server error. It never automatically retries
`POST`, `PUT`, `PATCH`, or `DELETE`, because the downstream operation may already have changed state before the
connection failed.

Each protected dependency has an independent count-based circuit breaker:

- the last 10 calls form the evaluation window;
- at least 5 calls are required before evaluation;
- a 50% failure rate opens the circuit;
- the circuit remains open for 15 seconds;
- two trial calls are allowed in half-open state.

External providers also have semaphore bulkheads with no waiting queue. Stripe accepts at most 10 concurrent Checkout
creations and SMTP at most 5 concurrent sends by default. Excess calls fail immediately without occupying every
application request or scheduler thread. Override these limits with `STRIPE_MAX_CONCURRENT_CALLS` and
`SMTP_MAX_CONCURRENT_CALLS` only after observing real workload behavior.

An open circuit fails immediately with HTTP `503` and a safe problem response. Booking operations do not use stale
or fabricated hotel availability and pricing as a fallback.

## Observing failures

Prometheus scrapes the Resilience4j Micrometer metrics. The provisioned Grafana overview includes:

- circuit-breaker state by application and dependency;
- failed calls and calls rejected by an open circuit;
- calls that succeeded or failed after retrying.
- available and maximum bulkhead capacity for Stripe and SMTP.

Useful raw metrics include:

- `resilience4j_circuitbreaker_state`;
- `resilience4j_circuitbreaker_calls_seconds_count`;
- `resilience4j_circuitbreaker_not_permitted_calls_total`;
- `resilience4j_retry_calls_total`.
- `resilience4j_bulkhead_available_concurrent_calls`;
- `resilience4j_bulkhead_max_allowed_concurrent_calls`.

## Local failure exercise

1. Open the Grafana overview dashboard.
2. Stop hotel-service while leaving booking-service and the gateway running.
3. Repeat an availability or hotel request at least five times.
4. Confirm that responses become fast `503` failures and the hotel-service circuit changes to open.
5. Start hotel-service again.
6. After 15 seconds, make two successful requests and confirm that the circuit closes.

This exercise should not be run against a production system with real guest traffic.
