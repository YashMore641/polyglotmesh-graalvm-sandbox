package com.polyglotmesh.service;

import com.polyglotmesh.config.GraalVMConfig;
import com.polyglotmesh.dto.PythonExecutionResult;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Service
public class PythonExecutionService {

    private final GraalVMConfig.PolyglotContextFactory contextFactory;

    public PythonExecutionService(GraalVMConfig.PolyglotContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }

    public PythonExecutionResult executePython(String code) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (Context context = contextFactory.createPythonContext(output)) {
            Value value = context.eval("python", code);
            String result = hasNoExpressionResult(value) ? null : value.toString();
            return new PythonExecutionResult(true, result, output.toString(StandardCharsets.UTF_8), null);
        } catch (PolyglotException exception) {
            String errorType = exception.isSyntaxError() ? "Syntax error: " : "Execution error: ";
            return new PythonExecutionResult(
                    false,
                    null,
                    output.toString(StandardCharsets.UTF_8),
                    errorType + exception.getMessage());
        }
    }

    private boolean hasNoExpressionResult(Value value) {
        if (value.isNull()) {
            return true;
        }
        Value metaObject = value.getMetaObject();
        return metaObject != null && "module".equals(metaObject.getMetaSimpleName());
    }
}
