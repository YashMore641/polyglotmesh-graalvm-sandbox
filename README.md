# PolyglotMesh

**PolyglotMesh – GraalVM Multi-Language Runtime Sandbox** is an individual internship project exploring how a backend can safely accept and manage scripts written in multiple languages through a common runtime platform. This repository is organized as a monorepo so the backend, future frontend, documentation, and sample scripts can evolve together.

> **Project status:** Day 7 development. The backend provides health and execution REST endpoints, with a common service for restricted JavaScript and Python execution through GraalVM Polyglot. Execution limits and security boundaries are configurable, while persistence and the React frontend remain unimplemented.

## Problem statement

Running scripts from different programming languages often requires separate runtimes, inconsistent interfaces, and careful isolation. PolyglotMesh aims to provide a unified application foundation for exploring multi-language execution with GraalVM while keeping future execution boundaries explicit and testable.

## Objectives

- Establish a maintainable Java 17+ and Spring Boot 3 backend.
- Prepare a monorepo layout for backend, frontend, documentation, and example scripts.
- Provide a small HTTP health check for local development and deployment checks.
- Build toward a common interface for future language execution and sandbox management.
- Execute JavaScript through a restricted, request-local GraalVM Polyglot context.
- Execute Python through a restricted, isolated GraalVM Polyglot context.
- Route JavaScript and Python requests through a common language execution service.
- Expose JavaScript and Python execution through a validated REST endpoint.
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

The controller, service, DTO, and exception packages contain the backend API and execution implementation. The model and repository packages remain reserved for future features.

## Architecture overview

The initial backend is a Spring WebFlux application. HTTP requests are handled by controllers, which will delegate business operations to services as the project grows. DTOs define API payloads; future repository adapters can isolate persistence concerns. The future React frontend will communicate with the backend over HTTP.

GraalVM's Polyglot API, JavaScript runtime, and GraalPy runtime are used by services that evaluate each script in a fresh context with host access, host class lookup, guest IO, and native access disabled. MongoDB is a future integration and is not configured in this scaffold.

## Technology stack

| Technology | Purpose |
|---|---|
| Java 17+ | Minimum backend runtime and language baseline |
| Spring Boot 3 | Application bootstrap and dependency management |
| Spring WebFlux | Reactive HTTP server and REST API foundation |
| Spring Boot Validation | Bean validation for future API input models |
| Maven | Backend build and dependency management |
| GraalVM Polyglot | Restricted JavaScript and Python execution with per-request context lifecycle |
| React | Planned frontend |
| MongoDB | Planned persistence integration |

### Backend dependencies

- `spring-boot-starter-webflux` supplies Spring's reactive web stack, HTTP routing, and the embedded server.
- `spring-boot-starter-validation` provides Jakarta Bean Validation for validating future request DTOs.
- `spring-boot-starter-test` provides Spring Boot and JUnit testing support.
- `reactor-test` provides testing utilities for Reactor publishers used by WebFlux.
- `polyglot` provides the GraalVM embedding API; `js-community` and `python-community` supply the JavaScript and GraalPy runtimes.

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

## Python execution

`PythonExecutionService` executes Python through GraalVM's Polyglot API. Each call creates and closes an isolated Python context, with Python `print()` output captured in the result.

```java
PythonExecutionResult printed =
        executionService.executePython("print(\"Hello from Python\")");
// printed.output(): "Hello from Python\n"

PythonExecutionResult calculated = executionService.executePython("10 + 20");
// calculated.result(): "30"
```

The Python runtime is supplied by the Maven `python-community` dependency; GraalPy is not launched as an external Python process. The tested examples do not imply compatibility with arbitrary Python programs or native packages. See [docs/graalvm-setup.md](docs/graalvm-setup.md) for GraalPy version, runtime packaging, and compatibility details.

## Execution API

Submit code to `POST http://localhost:8080/api/execute` with a JSON body containing `language` and `code`.

Supported language values are `javascript`, `js`, `python`, and `py` (case-insensitive; surrounding whitespace is ignored). Both fields must be non-blank, and code is limited to 100,000 characters.

JavaScript request:

```json
{
  "language": "javascript",
  "code": "console.log('Hello from PolyglotMesh');"
}
```

Python request:

```json
{
  "language": "python",
  "code": "print('Hello from Python')"
}
```

Successful requests return HTTP `200 OK` with the execution response:

```json
{
  "success": true,
  "language": "javascript",
  "output": "Hello from PolyglotMesh\n",
  "error": null,
  "executionTimeMs": 12
}
```

Invalid JSON, missing or blank fields, unsupported languages, or code over the size limit return HTTP `400 Bad Request`. The response uses the same fields, with `success` set to `false` and the reason in `error`. A valid request whose code fails during execution (for example, a syntax error) still returns HTTP `200 OK`; inspect `success` and `error` in the response to determine the execution outcome.

## Common multi-language execution service

`PolyglotExecutionService.execute(String language, String code)` routes `javascript`/`js` to `JavaScriptExecutionService` and `python`/`py` to `PythonExecutionService`. Language matching ignores case and surrounding whitespace. It returns an `ExecutionResponse` containing `success`, the requested language, `output`, `error`, and `executionTimeMs`. Output is captured guest output when present, or otherwise the final expression result. Invalid code and unsupported or invalid inputs are returned as failed responses rather than escaping as guest execution exceptions. Source code is limited to 100,000 characters.

The controller delegates execution to this service:

```java
ExecutionResponse response = polyglotExecutionService.execute(
        "javascript",
        "console.log('Hello');"
);
// success=true, language="javascript", output="Hello\n", error=null

ExecutionResponse calculation = polyglotExecutionService.execute("py", "10 + 20");
// success=true, language="py", output="30", error=null
```

Each language-specific service creates and closes a fresh context per execution; guest contexts have no host access, host class lookup, IO, native access, or thread creation. These restrictions do not impose CPU or memory limits: an infinite loop or memory-intensive program can still consume resources or block an application thread. Context isolation alone is not a complete sandbox. Before exposing the execution endpoint to untrusted users, execute scripts in separately constrained processes or containers with explicit CPU, memory, and wall-clock limits.

## Day 7 security boundaries

GraalVM contexts are created separately for JavaScript and Python and are restricted as follows:

- `allowAllAccess(false)` is used; `allowAllAccess(true)` is never enabled.
- Host access is set to `HostAccess.NONE`.
- Host class lookup is denied.
- Guest IO uses `IOAccess.NONE`, so filesystem access and standard process IO are not exposed.
- Native access and guest-created threads are disabled.
- JavaScript and Python each receive a separate context-construction path and request-local output stream.
- Each context shares only the reusable engine; guest bindings and variables do not persist between requests.

The following limits are configurable in `backend/src/main/resources/application.properties`:

```properties
polyglotmesh.security.max-source-length=100000
polyglotmesh.security.execution-timeout-ms=30000
polyglotmesh.security.statement-limit=0
```

`max-source-length` rejects oversized source in the REST service and language-specific services. `statement-limit` applies GraalVM resource limits to guest execution. `execution-timeout-ms` schedules GraalVM context interruption for the wall-clock timeout when the selected runtime supports context interruption. Set any time or statement limit to `0` to disable that corresponding configured limit.

Timeout, cancellation, resource-exhaustion, syntax, runtime, and security restriction failures are returned as structured execution responses. For example, timeout/cancellation uses `Execution timeout:`, resource exhaustion uses `Execution limit exceeded:`, and blocked host access uses `Security restriction:` where the Polyglot API exposes that category.

These controls reduce accidental or direct host access and provide bounded request validation and guest execution limits. They are not a complete security boundary. GraalVM Context isolation does not guarantee safe CPU, memory, native-process, or denial-of-service isolation. A guest can still consume host CPU or memory, consume large output, trigger runtime bugs, or affect availability in ways not prevented by these limits. Do not expose filesystem, environment variables, network sockets, host processes, or Java host access to guest code.

Before accepting untrusted scripts in production, run each execution in an isolated process or container with enforced CPU, memory, output, process, and wall-clock limits, plus network, filesystem, and privilege restrictions. The timeout behavior depends on guest-language safepoints and the selected GraalVM JDK/runtime, so wall-clock interruption must be defense-in-depth rather than the only limit.

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
