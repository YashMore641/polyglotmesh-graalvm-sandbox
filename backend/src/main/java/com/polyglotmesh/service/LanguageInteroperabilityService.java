package com.polyglotmesh.service;

import com.polyglotmesh.config.GraalVMConfig;
import com.polyglotmesh.dto.InteroperabilityRequest;
import com.polyglotmesh.dto.InteroperabilityResponse;
import com.polyglotmesh.exception.InteroperabilityException;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class LanguageInteroperabilityService {

    private final GraalVMConfig.PolyglotContextFactory contextFactory;

    public LanguageInteroperabilityService(GraalVMConfig.PolyglotContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }

    public InteroperabilityResponse exchange(InteroperabilityRequest request) {
        long startTime = System.nanoTime();
        String sourceLanguage = normalizeLanguage(request.sourceLanguage());
        String targetLanguage = normalizeLanguage(request.targetLanguage());
        if (sourceLanguage.equals(targetLanguage)) {
            throw badRequest("Source and target languages must be different.");
        }
        validateCode(request.sourceCode());
        validateCode(request.targetCode());

        ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
        try {
            PrimitiveValue transferredValue = evaluateSource(
                    sourceLanguage, request.sourceCode(), timeoutExecutor);
            String targetResult = evaluateTarget(
                    targetLanguage, request.targetCode(), transferredValue, timeoutExecutor);

            return new InteroperabilityResponse(
                    true,
                    sourceLanguage,
                    targetLanguage,
                    transferredValue.type(),
                    transferredValue.displayValue(),
                    targetResult,
                    elapsedTimeMs(startTime));
        } catch (PolyglotException exception) {
            throw executionFailure(exception);
        } finally {
            timeoutExecutor.shutdownNow();
        }
    }

    private PrimitiveValue evaluateSource(
            String language,
            String code,
            ScheduledExecutorService timeoutExecutor) {
        try (Context context = createContext(language)) {
            Future<?> timeout = scheduleTimeout(timeoutExecutor, context);
            try {
                return toPrimitive(context.eval(language, code));
            } finally {
                timeout.cancel(true);
            }
        }
    }

    private String evaluateTarget(
            String language,
            String code,
            PrimitiveValue value,
            ScheduledExecutorService timeoutExecutor) {
        try (Context context = createContext(language)) {
            context.getBindings(language).putMember("sharedValue", value.value());
            Future<?> timeout = scheduleTimeout(timeoutExecutor, context);
            try {
                return context.eval(language, code).toString();
            } finally {
                timeout.cancel(true);
            }
        }
    }

    private Context createContext(String language) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        return "js".equals(language)
                ? contextFactory.createJavaScriptContext(output)
                : contextFactory.createPythonContext(output);
    }

    private PrimitiveValue toPrimitive(Value value) {
        if (value.isString()) {
            String stringValue = value.asString();
            if (stringValue.length() > contextFactory.getMaxSourceLength()) {
                throw invalidValue("String value exceeds the maximum supported size of "
                        + contextFactory.getMaxSourceLength() + " characters.");
            }
            return new PrimitiveValue(stringValue, "string", stringValue);
        }
        if (value.isNumber() && value.fitsInDouble()) {
            double number = value.asDouble();
            if (Double.isFinite(number)) {
                return new PrimitiveValue(number, "number", Double.toString(number));
            }
        }
        throw invalidValue("Only strings and finite, double-representable numbers can be shared.");
    }

    private void validateCode(String code) {
        int maxSourceLength = contextFactory.getMaxSourceLength();
        if (code.length() > maxSourceLength) {
            throw new InteroperabilityException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "CODE_TOO_LARGE",
                    "Code exceeds the maximum size of " + maxSourceLength + " characters.");
        }
    }

    private String normalizeLanguage(String language) {
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "javascript", "js" -> "js";
            case "python", "py" -> "python";
            default -> throw badRequest("Unsupported language: " + language);
        };
    }

    private Future<?> scheduleTimeout(ScheduledExecutorService executor, Context context) {
        long timeoutMs = contextFactory.getExecutionTimeoutMs();
        if (timeoutMs <= 0) {
            return executor.schedule(() -> { }, 1, TimeUnit.DAYS);
        }
        return executor.schedule(() -> {
            try {
                context.interrupt(Duration.ofMillis(250));
            } catch (TimeoutException | IllegalStateException ignored) {
                // The guest may need another safepoint before the interrupt is confirmed.
            }
        }, timeoutMs, TimeUnit.MILLISECONDS);
    }

    private InteroperabilityException executionFailure(PolyglotException exception) {
        if (exception.isCancelled() || exception.isInterrupted()) {
            return new InteroperabilityException(
                    HttpStatus.GATEWAY_TIMEOUT, "EXECUTION_TIMEOUT", "Execution timeout: " + exception.getMessage());
        }
        if (exception.isResourceExhausted()) {
            return new InteroperabilityException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "EXECUTION_LIMIT_EXCEEDED",
                    "Execution limit exceeded: " + exception.getMessage());
        }
        if (exception.isHostException()) {
            return new InteroperabilityException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "SECURITY_RESTRICTION",
                    "Security restriction: " + exception.getMessage());
        }
        return new InteroperabilityException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "INTEROPERABILITY_EXECUTION_ERROR",
                (exception.isSyntaxError() ? "Syntax error: " : "Execution error: ") + exception.getMessage());
    }

    private InteroperabilityException invalidValue(String message) {
        return new InteroperabilityException(
                HttpStatus.UNPROCESSABLE_ENTITY, "UNSUPPORTED_VALUE_TYPE", message);
    }

    private InteroperabilityException badRequest(String message) {
        return new InteroperabilityException(HttpStatus.BAD_REQUEST, "INVALID_INTEROPERABILITY_REQUEST", message);
    }

    private long elapsedTimeMs(long startTime) {
        return (System.nanoTime() - startTime) / 1_000_000;
    }

    private record PrimitiveValue(Object value, String type, String displayValue) {
    }
}
