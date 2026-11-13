# GraalVM Polyglot Setup

## Compatibility choices

The backend targets Java 17 and Spring Boot 3.5.7. Spring Boot 3.5 requires Java 17 or newer. The Polyglot API and both language runtime dependencies use GraalVM component version `24.1.2`; the matching `js-community` and `python-community` artifacts are selected to keep all GraalVM components on one release line. The community artifacts avoid the Oracle GraalVM Free Terms and Conditions (GFTC) language artifacts.

GraalVM's release notes identify language components `24.1.2` as designed for GraalVM for JDK 23. The Maven artifacts' base classes target Java 17 bytecode (with some multi-release classes for newer JDKs), so this component line preserves the project's Java 17 bytecode baseline. The supported GraalVM JDK pairing is not the same thing as the Java version targeted by this project. The project tests were run on Java 24.0.1 in this environment, not on a Java 17 runtime; run the test suite on the exact JDK distribution selected for deployment.

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

`GraalVMConfig` exposes a singleton `Engine` and a context factory. Each call to `createContext()` creates a fresh context for JavaScript and Python, sharing the engine's reusable compilation resources without sharing context state. Callers must close contexts (for example, with try-with-resources).

Host access is explicitly disabled, Java host class lookup is denied, guest IO and native access are disabled, and guest-created threads are disabled. `allowAllAccess(true)` is not used. These settings are a starting boundary, not a complete sandbox for untrusted scripts; additional resource limits and process/container isolation are needed before accepting untrusted workloads.

## References

- [GraalVM: Embedding Languages](https://www.graalvm.org/latest/reference-manual/embed-languages/) — Maven dependency setup, language POM artifacts, and context lifecycle.
- [GraalVM for JDK 23.0.2 release notes](https://www.graalvm.org/release-notes/JDK_23/) — identifies Truffle language/component version 24.1.2 as designed for GraalVM for JDK 23.0.2.
- [Spring Boot 3.5 system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html) — Java and build-tool requirements.
