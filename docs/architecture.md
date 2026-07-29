# Architecture

## Initial direction

The system will start as a small set of independently runnable services:

- **Identity Service** — customer accounts, staff users, roles, and authentication.
- **Hotel Service** — hotels, rooms, room types, and rates.
- **Booking Service** — availability, reservations, guest access links, and booking lifecycle.
- **Notification Service** — asynchronous email notifications.

Guest booking must not require account creation. A guest receives a secure link by email to view an eligible reservation. The link remains valid until checkout plus a configurable grace period; guest modifications initially require staff assistance.

Room types are hotel-owned in the initial model. A future larger-scale design could introduce a global room-type catalog plus a hotel-specific offering table, but that would add complexity without a current product benefit.

Room types are hotel-owned in the initial model. A future larger-scale design could introduce a global room-type catalog plus a hotel-specific offering table, but that would add complexity without a current product benefit.

## Principles

1. Each service owns its data and exposes an API.
2. Cross-service communication uses APIs or events, never another service's database.
3. The first release prioritizes one hotel and a narrow booking workflow.
4. Security, auditability, and operational visibility are part of the design from the beginning.
5. Notifications are isolated in `notification-service`; reservation events use an internal event contract now and can be connected to SNS/SQS later.
6. Local service-to-service event delivery uses a configurable internal service token; production deployment should replace this with stronger workload identity or mTLS.
7. Draft PostgreSQL V1 schema scripts are maintained for all services, but H2 with `ddl-auto: update` remains active until PostgreSQL is introduced.
8. Local PostgreSQL is provisioned through Docker Compose as one PostgreSQL instance with separate databases per service; application services continue using H2 until the PostgreSQL profile is explicitly enabled.
