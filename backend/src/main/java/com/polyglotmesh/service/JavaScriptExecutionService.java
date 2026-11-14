package com.polyglotmesh.service;

import com.polyglotmesh.config.GraalVMConfig;
import com.polyglotmesh.dto.JavaScriptExecutionResult;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Service
public class JavaScriptExecutionService {

    private final GraalVMConfig.PolyglotContextFactory contextFactory;

    public JavaScriptExecutionService(GraalVMConfig.PolyglotContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }

    public JavaScriptExecutionResult executeJavaScript(String code) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (Context context = contextFactory.createJavaScriptContext(output)) {
            String result = context.eval("js", code).toString();
            return new JavaScriptExecutionResult(true, result, output.toString(StandardCharsets.UTF_8), null);
        } catch (PolyglotException exception) {
            String errorType = exception.isSyntaxError() ? "Syntax error: " : "Execution error: ";
            return new JavaScriptExecutionResult(
                    false,
                    null,
                    output.toString(StandardCharsets.UTF_8),
                    errorType + exception.getMessage());
        }
    }
}
