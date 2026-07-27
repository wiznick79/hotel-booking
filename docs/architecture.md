# Architecture

## Initial direction

The system will start as a small set of independently runnable services:

- **Identity Service** — customer accounts, staff users, roles, and authentication.
- **Hotel Service** — hotels, rooms, room types, and rates.
- **Booking Service** — availability, reservations, guest access links, and booking lifecycle.
- **Notification Service** — asynchronous email notifications.

Guest booking must not require account creation. A guest receives a secure, expiring link by email to view or manage an eligible reservation.

## Principles

1. Each service owns its data and exposes an API.
2. Cross-service communication uses APIs or events, never another service's database.
3. The first release prioritizes one hotel and a narrow booking workflow.
4. Security, auditability, and operational visibility are part of the design from the beginning.

