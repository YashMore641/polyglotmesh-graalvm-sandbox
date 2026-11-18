package com.polyglotmesh.service;

import com.polyglotmesh.config.GraalVMConfig;
import com.polyglotmesh.dto.ExecutionResponse;
import com.polyglotmesh.dto.JavaScriptExecutionResult;
import com.polyglotmesh.dto.PythonExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class PolyglotExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(PolyglotExecutionService.class);

    private final JavaScriptExecutionService javaScriptExecutionService;
    private final PythonExecutionService pythonExecutionService;
    private final GraalVMConfig.PolyglotContextFactory contextFactory;

    public PolyglotExecutionService(
            JavaScriptExecutionService javaScriptExecutionService,
            PythonExecutionService pythonExecutionService,
            GraalVMConfig.PolyglotContextFactory contextFactory) {
        this.javaScriptExecutionService = javaScriptExecutionService;
        this.pythonExecutionService = pythonExecutionService;
        this.contextFactory = contextFactory;
    }

    public ExecutionResponse execute(String language, String code) {
        long startTime = System.nanoTime();
        if (language == null || language.isBlank()) {
            return failure(language, "Language must not be empty.", startTime);
        }
        if (code == null || code.isBlank()) {
            return failure(language.trim().toLowerCase(Locale.ROOT), "Code must not be empty.", startTime);
        }
        int maxSourceLength = contextFactory.getMaxSourceLength();
        if (code.length() > maxSourceLength) {
            return failure(
                    language.trim().toLowerCase(Locale.ROOT),
                    "Code exceeds the maximum size of " + maxSourceLength + " characters.",
                    startTime);
        }

        String normalizedLanguage = language.trim().toLowerCase(Locale.ROOT);
        try {
            return switch (normalizedLanguage) {
                case "javascript", "js" -> toResponse(
                        normalizedLanguage,
                        javaScriptExecutionService.executeJavaScript(code),
                        startTime);
                case "python", "py" -> toResponse(
                        normalizedLanguage,
                        pythonExecutionService.executePython(code),
                        startTime);
                default -> failure(
                        normalizedLanguage,
                        "Unsupported language: " + language
                                + ". Supported languages: javascript, js, python, py.",
                        startTime);
            };
        } catch (RuntimeException exception) {
            logger.error("Unexpected failure while executing {} code.", normalizedLanguage, exception);
            return failure(
                    normalizedLanguage,
                    "Execution failed: " + (exception.getMessage() == null
                            ? exception.getClass().getSimpleName()
                            : exception.getMessage()),
                    startTime);
        }
    }

    private ExecutionResponse toResponse(
            String requestedLanguage,
            JavaScriptExecutionResult result,
            long startTime) {
        return new ExecutionResponse(
                result.success(),
                requestedLanguage,
                result.output().isEmpty() ? result.result() : result.output(),
                result.error(),
                elapsedTimeMs(startTime));
    }

    private ExecutionResponse toResponse(
            String requestedLanguage,
            PythonExecutionResult result,
            long startTime) {
        return new ExecutionResponse(
                result.success(),
                requestedLanguage,
                result.output().isEmpty() ? result.result() : result.output(),
                result.error(),
                elapsedTimeMs(startTime));
    }

    private ExecutionResponse failure(String language, String error, long startTime) {
        return new ExecutionResponse(false, language, "", error, elapsedTimeMs(startTime));
    }

    private long elapsedTimeMs(long startTime) {
        return (System.nanoTime() - startTime) / 1_000_000;
    }
}
