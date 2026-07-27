# Architecture Decisions

## ADR-001: Separate services from the beginning

We will use a small microservices architecture from the start because understanding service boundaries, communication, deployment, and operational concerns is an explicit project goal.

The number of services will remain deliberately small to avoid turning infrastructure complexity into the project itself.

## ADR-002: Java 21

Java 21 is the project baseline because it is an LTS release with broad Spring Boot and industry adoption. Java 25 remains a possible future upgrade exercise.

