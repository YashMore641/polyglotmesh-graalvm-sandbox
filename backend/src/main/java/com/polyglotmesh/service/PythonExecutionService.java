package com.polyglotmesh.service;

import com.polyglotmesh.config.GraalVMConfig;
import com.polyglotmesh.dto.PythonExecutionResult;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class PythonExecutionService {

    private final GraalVMConfig.PolyglotContextFactory contextFactory;

    public PythonExecutionService(GraalVMConfig.PolyglotContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }

    public PythonExecutionResult executePython(String code) {
        int maxSourceLength = contextFactory.getMaxSourceLength();
        if (code == null || code.isBlank()) {
            return new PythonExecutionResult(false, null, "", "Code must not be empty.");
        }
        if (code.length() > maxSourceLength) {
            return new PythonExecutionResult(
                    false,
                    null,
                    "",
                    "Code exceeds the maximum size of " + maxSourceLength + " characters.");
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
        try (Context context = contextFactory.createPythonContext(output)) {
            Future<?> timeout = scheduleTimeout(timeoutExecutor, context);
            Value value = context.eval("python", code);
            timeout.cancel(true);
            String result = hasNoExpressionResult(value) ? null : value.toString();
            return new PythonExecutionResult(true, result, output.toString(StandardCharsets.UTF_8), null);
        } catch (PolyglotException exception) {
            String errorType = classifyException(exception);
            return new PythonExecutionResult(
                    false,
                    null,
                    output.toString(StandardCharsets.UTF_8),
                    errorType + exception.getMessage());
        } finally {
            timeoutExecutor.shutdownNow();
        }
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

    private String classifyException(PolyglotException exception) {
        if (exception.isCancelled() || exception.isInterrupted()) {
            return "Execution timeout: ";
        }
        if (exception.isResourceExhausted()) {
            return "Execution limit exceeded: ";
        }
        if (exception.isSyntaxError()) {
            return "Syntax error: ";
        }
        if (exception.isHostException()) {
            return "Security restriction: ";
        }
        String message = exception.getMessage();
        if (message != null
                && message.matches("(?i).*\\b(host access|host class|native access|java\\.type|IO|network|environment)\\b.*")) {
            return "Security restriction: ";
        }
        return "Execution error: ";
    }

    private boolean hasNoExpressionResult(Value value) {
        if (value.isNull()) {
            return true;
        }
        Value metaObject = value.getMetaObject();
        return metaObject != null && "module".equals(metaObject.getMetaSimpleName());
    }
}
