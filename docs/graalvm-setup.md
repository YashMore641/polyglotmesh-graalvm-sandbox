# GraalVM Polyglot Setup

## Compatibility choices

The backend targets Java 17 and Spring Boot 3.5.7. Spring Boot 3.5 requires Java 17 or newer. The Polyglot API and both language runtime dependencies use GraalVM component version `24.1.2`; the matching `js-community` and `python-community` artifacts are selected to keep all GraalVM components on one release line. The community artifacts avoid the Oracle GraalVM Free Terms and Conditions (GFTC) language artifacts.

GraalVM's release notes identify language components `24.1.2` as designed for GraalVM for JDK 23. The Maven artifacts' base classes target Java 17 bytecode (with some multi-release classes for newer JDKs), so this component line preserves the project's Java 17 bytecode baseline. The supported GraalVM JDK pairing is not the same thing as the Java version targeted by this project. The project tests were run on Oracle HotSpot JDK 24.0.1, not on GraalVM JDK or Java 17. The Python and JavaScript tests passed, but GraalVM warned that JVMCI was disabled and fell back to interpreter-only execution. Run the tests on the exact JDK distribution selected for deployment; the optimized GraalVM JDK pairing remains to be validated in this project.

## JVM and language runtimes

The JVM runs the Spring Boot application. A GraalVM JDK is a JVM distribution that also provides the GraalVM compiler and related tools. The JavaScript and Python guest-language implementations are separate from the JVM: Maven resolves them through the `js-community` and `python-community` runtime dependencies. Adding those dependencies makes the languages available to the application, but does not install or switch the machine's JDK to GraalVM.

The Maven configuration pins the Polyglot API and guest languages using the single `graalvm.polyglot.version` property in `backend/pom.xml`. The language dependencies are POM artifacts by design and transitively bring their language engines and runtime resources.

## Install and select a GraalVM JDK (optional)

The application can be built with a Java 17-compatible JDK. To use the GraalVM JIT compiler with the selected language-component line, install GraalVM for JDK 23 (the pairing identified in the release notes) and select it for your shell or IDE:

1. Download the appropriate GraalVM JDK for your OS and architecture from the [official GraalVM downloads](https://www.graalvm.org/downloads/).
2. Set `JAVA_HOME` to the installed GraalVM JDK and add `%JAVA_HOME%\bin` to `PATH` in Windows (or `$JAVA_HOME/bin` on macOS/Linux).
3. Verify with `java -version` and `mvn -version`; both should report the intended JDK.
4. From `backend`, run `mvn clean test`.

Installing a separate GraalVM JDK is not needed just to resolve or load the Maven language artifacts. A GraalVM JDK is needed when GraalVM JVM compiler behavior is desired. JDK 23 satisfies the project's Java 17+ requirement, but if deployment must use exactly JDK 17, validate there before release; the current local test environment did not include JDK 17.

## Polyglot context security and lifecycle

`GraalVMConfig` exposes a singleton `Engine` and a context factory. Each call to `createContext()` creates a fresh context for JavaScript and Python, and `createJavaScriptContext(OutputStream)` creates a JavaScript-only context that routes standard output and error to the supplied stream. Contexts share the engine's reusable compilation resources without sharing context state. The execution service creates a new context for every request and closes it with try-with-resources.

Host access is explicitly disabled, Java host class lookup is denied, guest IO and native access are disabled, and guest-created threads are disabled. `allowAllAccess(true)` is not used. These settings are a starting boundary, not a complete sandbox for untrusted scripts; additional resource limits and process/container isolation are needed before accepting untrusted workloads.

Day 7 adds configurable source-length, statement, and wall-clock controls:

```properties
polyglotmesh.security.max-source-length=100000
polyglotmesh.security.execution-timeout-ms=30000
polyglotmesh.security.statement-limit=0
```

JavaScript and Python use separate context factories, request-local output streams, and the same explicit deny-by-default host and IO policy. Resource limits apply through the Polyglot API; context interruption is scheduled for the configured wall-clock timeout. Timeout/cancellation, resource exhaustion, syntax, runtime, and blocked host access are categorized in the language services. A zero statement or timeout value disables that configured limit.

The limits in this repository are application-level safeguards, not proof of isolation from CPU, memory, native runtime, or denial-of-service attacks. Context isolation alone is not a complete sandbox. Production untrusted execution should use separate constrained processes or containers with CPU, memory, output, process, filesystem, network, and wall-clock enforcement. Wall-clock interruption is language/runtime-dependent and must not be treated as a complete DoS defense.

## JavaScript execution service

`JavaScriptExecutionService.executeJavaScript(String)` evaluates one JavaScript source string and returns a `JavaScriptExecutionResult` with `success`, `result`, `output`, and `error` fields. The result is GraalVM's readable representation of the last evaluated value (for example, `"30"`); scripts that only print have the result `"undefined"`. Console output is captured per request. Syntax and runtime failures return `success: false` and a descriptive error instead of escaping as exceptions.

Examples:

```java
executionService.executeJavaScript("console.log(\"Hello from PolyglotMesh\");");
// success=true, result="undefined", output="Hello from PolyglotMesh"

executionService.executeJavaScript("10 + 20");
// success=true, result="30", output=""

executionService.executeJavaScript("const a = 15; const b = 25; a + b");
// success=true, result="40", output=""
```

The `Context` API evaluates code through `context.eval("js", code)`. The service reads the resulting `Value` with `toString()`, captures guest output in a request-local `ByteArrayOutputStream`, and relies on try-with-resources to close each context even when evaluation throws.

## GraalPy configuration and Python execution

The backend includes the `org.graalvm.polyglot:python-community` runtime POM at the same `24.1.2` version as the Polyglot API and JavaScript runtime. Maven resolves GraalPy's language implementation and runtime resources; a separate CPython installation is not used. The runtime dependencies must remain on the application runtime classpath, including when packaging or deploying the Spring Boot application.

`PythonExecutionService.executePython(String)` creates a fresh Python-only context per call, captures standard output and error, and closes the context after evaluation. It uses the same restricted context builder as the other language contexts: host access, host class lookup, guest IO, native access, and thread creation are disabled. Python `None` has no expression result and is returned as `null`; other final expression values are returned as strings. Syntax and runtime failures return a failed result with a categorized error.

Examples:

```java
executionService.executePython("print(\"Hello from Python\")");
// success=true, result=null, output="Hello from Python\n"

executionService.executePython("print(10 + 20)");
// success=true, result=null, output="30\n"

executionService.executePython("numbers = [10, 20, 30]\nprint(sum(numbers))");
// success=true, output="60\n"

executionService.executePython("10 + 20");
// success=true, result="30", output=""
```

The automated tests verify these examples, syntax and runtime error reporting, and state isolation between executions. This verifies the bundled GraalPy runtime on the tested JVM; it does not establish support for arbitrary third-party Python distributions, native Python extensions, or arbitrary Python programs. No extra package installation is needed for the built-in features exercised by these examples. Python package installation and native-extension support require separate compatibility and security validation. GraalPy `24.1.2` is designed for GraalVM for JDK 23 according to the linked release notes; the project test environment used Java 24.0.1, so that successful run alone does not certify every JVM distribution/version combination. Validate on the exact JDK and packaging path used in deployment.

## Common execution service

`PolyglotExecutionService.execute(String language, String code)` routes `javascript` or `js` to the JavaScript service and `python` or `py` to the Python service. Matching is case-insensitive and ignores surrounding whitespace. The service rejects blank inputs, limits source code to 100,000 characters, and returns an `ExecutionResponse` with execution status, requested language, captured output (or the final expression value when there is no captured output), error details, and elapsed time in milliseconds. There is not yet a REST endpoint for this service.

Language execution uses a new, restricted context for each call and closes it after evaluation. This does not impose CPU, memory, or wall-clock limits. Guest scripts can still run indefinitely or consume excessive memory, so context isolation and disabled host access are not sufficient protections for untrusted workloads. Add process/container isolation and explicit resource limits before accepting untrusted code.

## Experimental JavaScript–Python value exchange

`LanguageInteroperabilityService` uses separate JS-only and Python-only contexts created with the same restricted context factory; it does not rely on one guest language accessing another language's bindings. The source expression is evaluated first. Its GraalVM `Value` is checked and converted in Java to either a `String` or a finite `double` that the Polyglot API reports as exactly representable. Java then sets the primitive in the target context's language bindings under the name `sharedValue`, and evaluates the target expression.

For example, `POST /api/interop` can evaluate `21 * 2` in JavaScript and `sharedValue + 1` in Python. `POST /api/execute` remains unchanged and does not invoke this service. The supported transfer types are strings and finite, double-representable numbers. Booleans, null/undefined, arrays/lists, maps/dictionaries, functions, arbitrary guest objects, and numbers that cannot be represented without loss are rejected. String values are capped at the configured maximum source length. Guest console output is discarded; the response contains the transferred primitive and target expression result. Evaluation and conversion failures are returned as structured API errors.

This Java-mediated approach deliberately avoids transferring GraalVM `Value` instances or assuming that objects from one guest runtime are safe or usable in the other. It uses `Context.getBindings(...).putMember(...)` to provide a Java primitive to the destination guest; it does not enable host access or host class lookup. The integration tests exercise this path against the project's aligned Polyglot, GraalJS, and GraalPy `24.1.2` dependencies. This is a narrow demonstration, not a general-purpose serializer or an expanded sandbox guarantee.

## References

- [GraalVM: Embedding Languages](https://www.graalvm.org/latest/reference-manual/embed-languages/) — Maven dependency setup, language POM artifacts, and context lifecycle.
- [GraalVM for JDK 23.0.2 release notes](https://www.graalvm.org/release-notes/JDK_23/) — identifies Truffle language/component version 24.1.2 as designed for GraalVM for JDK 23.0.2.
- [Spring Boot 3.5 system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html) — Java and build-tool requirements.
