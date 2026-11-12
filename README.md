# PolyglotMesh

**PolyglotMesh – GraalVM Multi-Language Runtime Sandbox** is an individual internship project exploring how a backend can safely accept and manage scripts written in multiple languages through a common runtime platform. This repository is organized as a monorepo so the backend, future frontend, documentation, and sample scripts can evolve together.

> **Project status:** Day 1 initialization. The backend currently provides a health endpoint only. GraalVM script execution, sandboxing, persistence, and the React frontend are not implemented.

## Problem statement

Running scripts from different programming languages often requires separate runtimes, inconsistent interfaces, and careful isolation. PolyglotMesh aims to provide a unified application foundation for exploring multi-language execution with GraalVM while keeping future execution boundaries explicit and testable.

## Objectives

- Establish a maintainable Java 17+ and Spring Boot 3 backend.
- Prepare a monorepo layout for backend, frontend, documentation, and example scripts.
- Provide a small HTTP health check for local development and deployment checks.
- Build toward a common interface for future language execution and sandbox management.
- Keep GraalVM execution and database integration out of the initial scaffold.

## Repository layout

```text
.
├── backend/          # Spring Boot WebFlux application
├── frontend/         # Reserved for the future React application
├── docs/             # Project and architecture documentation
└── sample-scripts/   # Reserved for example polyglot scripts
```

The backend Java package namespace is `com.polyglotmesh`, with the following areas reserved for the application:

```text
com.polyglotmesh
├── config       # Application and runtime configuration
├── controller   # HTTP endpoints
├── service      # Business logic
├── model        # Domain models
├── dto          # API request and response types
├── exception    # Error handling
└── repository   # Persistence adapters
```

Only packages needed by the initial health endpoint contain source files. The remaining package directories are reserved for later features.

## Architecture overview

The initial backend is a Spring WebFlux application. HTTP requests are handled by controllers, which will delegate business operations to services as the project grows. DTOs define API payloads; future repository adapters can isolate persistence concerns. The future React frontend will communicate with the backend over HTTP.

GraalVM language runtimes and execution services are a planned part of the architecture, not part of this initialization. MongoDB is also a future integration and is not configured in this scaffold.

## Technology stack

| Technology | Purpose |
|---|---|
| Java 17+ | Minimum backend runtime and language baseline |
| Spring Boot 3 | Application bootstrap and dependency management |
| Spring WebFlux | Reactive HTTP server and REST API foundation |
| Spring Boot Validation | Bean validation for future API input models |
| Maven | Backend build and dependency management |
| GraalVM | Planned multi-language runtime; execution is not implemented yet |
| React | Planned frontend |
| MongoDB | Planned persistence integration |

### Backend dependencies

- `spring-boot-starter-webflux` supplies Spring's reactive web stack, HTTP routing, and the embedded server.
- `spring-boot-starter-validation` provides Jakarta Bean Validation for validating future request DTOs.
- `spring-boot-starter-test` provides Spring Boot and JUnit testing support.
- `reactor-test` provides testing utilities for Reactor publishers used by WebFlux.

## Health endpoint

```http
GET http://localhost:8080/api/health
```

Response:

```json
{
  "status": "UP",
  "application": "PolyglotMesh"
}
```

## Prerequisites

- JDK 17 or newer
- Apache Maven 3.6.3 or newer

## Run the backend

From the repository root, run:

```powershell
cd backend
mvn spring-boot:run
```

Then open `http://localhost:8080/api/health`.

Run the backend tests:

```powershell
cd backend
mvn test
```

Build the executable JAR:

```powershell
cd backend
mvn clean package
```

Run the packaged application:

```powershell
cd backend
java -jar target/polyglotmesh-backend-0.0.1-SNAPSHOT.jar
```
