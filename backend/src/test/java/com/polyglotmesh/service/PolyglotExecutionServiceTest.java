package com.polyglotmesh.service;

import com.polyglotmesh.dto.ExecutionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PolyglotExecutionServiceTest {

    @Autowired
    private PolyglotExecutionService executionService;

    @Test
    void executesJavaScriptAndReportsExecutionTime() {
        ExecutionResponse response = executionService.execute("javascript", "10 + 20");

        assertTrue(response.success());
        assertEquals("javascript", response.language());
        assertEquals("30", response.output());
        assertTrue(response.executionTimeMs() >= 0);
        assertNull(response.error());
    }

    @Test
    void executesPythonAlias() {
        ExecutionResponse response = executionService.execute("py", "print(10 + 20)");

        assertTrue(response.success());
        assertEquals("py", response.language());
        assertEquals("30\n", response.output());
        assertTrue(response.executionTimeMs() >= 0);
    }

    @Test
    void reportsInvalidJavaScript() {
        ExecutionResponse response = executionService.execute("js", "const = ;");

        assertFalse(response.success());
        assertNotNull(response.error());
        assertTrue(response.error().startsWith("Syntax error: "));
    }

    @Test
    void reportsInvalidPython() {
        ExecutionResponse response = executionService.execute("python", "if True print('missing colon')");

        assertFalse(response.success());
        assertNotNull(response.error());
        assertTrue(response.error().startsWith("Syntax error: "));
    }

    @Test
    void rejectsUnsupportedLanguage() {
        ExecutionResponse response = executionService.execute("ruby", "puts 'not supported'");

        assertFalse(response.success());
        assertTrue(response.error().contains("Unsupported language: ruby"));
    }

    @Test
    void rejectsEmptyLanguageAndCode() {
        ExecutionResponse emptyLanguage = executionService.execute(" ", "1 + 1");
        ExecutionResponse emptyCode = executionService.execute("js", " \t\n");

        assertFalse(emptyLanguage.success());
        assertEquals("Language must not be empty.", emptyLanguage.error());
        assertFalse(emptyCode.success());
        assertEquals("Code must not be empty.", emptyCode.error());
    }

    @Test
    void rejectsSourceCodeOverMaximumSize() {
        String oversizedCode = "a".repeat(100_001);

        ExecutionResponse response = executionService.execute("javascript", oversizedCode);

        assertFalse(response.success());
        assertTrue(response.error().contains("maximum size"));
    }
}
