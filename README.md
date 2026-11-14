# PolyglotMesh

**PolyglotMesh – GraalVM Multi-Language Runtime Sandbox** is an individual internship project exploring how a backend can safely accept and manage scripts written in multiple languages through a common runtime platform. This repository is organized as a monorepo so the backend, future frontend, documentation, and sample scripts can evolve together.

> **Project status:** Day 3 development. The backend provides a health endpoint and restricted JavaScript execution through GraalVM Polyglot. Persistence and the React frontend are not implemented.

## Problem statement

Running scripts from different programming languages often requires separate runtimes, inconsistent interfaces, and careful isolation. PolyglotMesh aims to provide a unified application foundation for exploring multi-language execution with GraalVM while keeping future execution boundaries explicit and testable.

## Objectives

- Establish a maintainable Java 17+ and Spring Boot 3 backend.
- Prepare a monorepo layout for backend, frontend, documentation, and example scripts.
- Provide a small HTTP health check for local development and deployment checks.
- Build toward a common interface for future language execution and sandbox management.
- Execute JavaScript through a restricted, request-local GraalVM Polyglot context.
- Keep database integration out of the current backend.

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

GraalVM's Polyglot API and JavaScript runtime are used by a service that evaluates each script in a fresh context with host access, host class lookup, guest IO, and native access disabled. MongoDB is a future integration and is not configured in this scaffold.

## Technology stack

| Technology | Purpose |
|---|---|
| Java 17+ | Minimum backend runtime and language baseline |
| Spring Boot 3 | Application bootstrap and dependency management |
| Spring WebFlux | Reactive HTTP server and REST API foundation |
| Spring Boot Validation | Bean validation for future API input models |
| Maven | Backend build and dependency management |
| GraalVM Polyglot | Restricted JavaScript execution with per-request context lifecycle |
| React | Planned frontend |
| MongoDB | Planned persistence integration |

### Backend dependencies

- `spring-boot-starter-webflux` supplies Spring's reactive web stack, HTTP routing, and the embedded server.
- `spring-boot-starter-validation` provides Jakarta Bean Validation for validating future request DTOs.
- `spring-boot-starter-test` provides Spring Boot and JUnit testing support.
- `reactor-test` provides testing utilities for Reactor publishers used by WebFlux.
- `polyglot` provides the GraalVM embedding API; `js-community` supplies the JavaScript runtime.

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

## JavaScript execution

The `JavaScriptExecutionService` executes source strings directly with the GraalVM Polyglot `Context` API. Each call creates and closes an independent context. Guest standard output and error are captured in the returned result, and syntax or execution failures are represented as structured errors.

```java
JavaScriptExecutionResult logged =
        executionService.executeJavaScript("console.log(\"Hello from PolyglotMesh\");");
// logged.output(): "Hello from PolyglotMesh" (followed by a newline)

JavaScriptExecutionResult calculated = executionService.executeJavaScript("10 + 20");
// calculated.result(): "30"
```

The result has `success`, `result`, `output`, and `error` fields. For example, invalid JavaScript returns `success: false` with a syntax-error message. This restricted context configuration is a starting point, not a complete isolation boundary for untrusted code; use process/container isolation and resource limits before exposing execution to untrusted workloads. See [docs/graalvm-setup.md](docs/graalvm-setup.md) for the configuration and API details.

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
