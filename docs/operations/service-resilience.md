# Service resilience

## Protected boundaries

The system currently has three synchronous resilience boundaries:

- API gateway to every downstream service;
- booking-service to hotel-service for catalogue, pricing, room, and availability data;
- notification-service to hotel-service for hotel-specific contact and sender data.

Kafka delivery already has separate retry and dead-letter behaviour and is not wrapped in these HTTP circuit
breakers.

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

An open circuit fails immediately with HTTP `503` and a safe problem response. Booking operations do not use stale
or fabricated hotel availability and pricing as a fallback.

## Observing failures

Prometheus scrapes the Resilience4j Micrometer metrics. The provisioned Grafana overview includes:

- circuit-breaker state by application and dependency;
- failed calls and calls rejected by an open circuit;
- calls that succeeded or failed after retrying.

Useful raw metrics include:

- `resilience4j_circuitbreaker_state`;
- `resilience4j_circuitbreaker_calls_seconds_count`;
- `resilience4j_circuitbreaker_not_permitted_calls_total`;
- `resilience4j_retry_calls_total`.

## Local failure exercise

1. Open the Grafana overview dashboard.
2. Stop hotel-service while leaving booking-service and the gateway running.
3. Repeat an availability or hotel request at least five times.
4. Confirm that responses become fast `503` failures and the hotel-service circuit changes to open.
5. Start hotel-service again.
6. After 15 seconds, make two successful requests and confirm that the circuit closes.

This exercise should not be run against a production system with real guest traffic.
