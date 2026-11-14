package com.polyglotmesh.service;

import com.polyglotmesh.dto.JavaScriptExecutionResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class JavaScriptExecutionServiceTest {

    @Autowired
    private JavaScriptExecutionService executionService;

    @Test
    void capturesConsoleOutput() {
        JavaScriptExecutionResult result =
                executionService.executeJavaScript("console.log(\"Hello from PolyglotMesh\");");

        assertTrue(result.success());
        assertEquals("Hello from PolyglotMesh", result.output().trim());
        assertEquals("undefined", result.result());
        assertNull(result.error());
    }

    @Test
    void returnsArithmeticExpressionResult() {
        JavaScriptExecutionResult result = executionService.executeJavaScript("10 + 20");

        assertTrue(result.success());
        assertEquals("30", result.result());
        assertEquals("", result.output());
    }

    @Test
    void returnsLastExpressionFromMultipleStatements() {
        JavaScriptExecutionResult result =
                executionService.executeJavaScript("const a = 15;\nconst b = 25;\na + b");

        assertTrue(result.success());
        assertEquals("40", result.result());
    }

    @Test
    void returnsMeaningfulErrorForInvalidSyntax() {
        JavaScriptExecutionResult result = executionService.executeJavaScript("const = ;");

        assertFalse(result.success());
        assertNull(result.result());
        assertNotNull(result.error());
        assertTrue(result.error().startsWith("Syntax error: "));
    }

    @Test
    void returnsMeaningfulErrorForRuntimeException() {
        JavaScriptExecutionResult result = executionService.executeJavaScript("throw new Error('test failure');");

        assertFalse(result.success());
        assertNull(result.result());
        assertTrue(result.error().startsWith("Execution error: "));
        assertTrue(result.error().contains("test failure"));
    }

    @Test
    void usesIndependentContextForEachExecution() {
        executionService.executeJavaScript("const requestLocal = 1;");

        JavaScriptExecutionResult nextExecution =
                executionService.executeJavaScript("typeof requestLocal");

        assertTrue(nextExecution.success());
        assertEquals("undefined", nextExecution.result());
    }
}
