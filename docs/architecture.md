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
