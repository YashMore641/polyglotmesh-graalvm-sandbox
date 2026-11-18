package com.polyglotmesh.service;

import com.polyglotmesh.dto.PythonExecutionResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PythonExecutionServiceTest {

    @Autowired
    private PythonExecutionService executionService;

    @Test
    void capturesPrintOutput() {
        PythonExecutionResult result = executionService.executePython("print(\"Hello from Python\")");

        assertTrue(result.success());
        assertEquals("Hello from Python", result.output().trim());
        assertNull(result.result());
        assertNull(result.error());
    }

    @Test
    void capturesPrintedArithmetic() {
        PythonExecutionResult result = executionService.executePython("print(10 + 20)");

        assertTrue(result.success());
        assertEquals("30", result.output().trim());
        assertNull(result.result());
    }

    @Test
    void runsPythonCollectionOperations() {
        PythonExecutionResult result =
                executionService.executePython("numbers = [10, 20, 30]\nprint(sum(numbers))");

        assertTrue(result.success());
        assertEquals("60", result.output().trim());
    }

    @Test
    void reportsInvalidSyntax() {
        PythonExecutionResult result = executionService.executePython("if True print('missing colon')");

        assertFalse(result.success());
        assertNull(result.result());
        assertNotNull(result.error());
        assertTrue(result.error().startsWith("Syntax error: "));
    }

    @Test
    void returnsExpressionResult() {
        PythonExecutionResult result = executionService.executePython("10 + 20");

        assertTrue(result.success());
        assertEquals("30", result.result());
        assertEquals("", result.output());
    }

    @Test
    void reportsRuntimeErrors() {
        PythonExecutionResult result = executionService.executePython("1 / 0");

        assertFalse(result.success());
        assertNull(result.result());
        assertTrue(result.error().startsWith("Execution error: "));
        assertTrue(result.error().contains("division"));
    }

    @Test
    void usesAnIndependentContextForEachExecution() {
        executionService.executePython("request_local = 42");

        PythonExecutionResult result = executionService.executePython("'request_local' in globals()");

        assertTrue(result.success());
        assertEquals("False", result.result());
    }

    @Test
    void rejectsOversizedSourceCode() {
        PythonExecutionResult result =
                executionService.executePython("x".repeat(100_001));

        assertFalse(result.success());
        assertTrue(result.error().contains("maximum size"));
    }
}
