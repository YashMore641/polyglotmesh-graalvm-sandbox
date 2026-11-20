package com.polyglotmesh.service;

import com.polyglotmesh.config.GraalVMConfig;
import com.polyglotmesh.config.GraalVMSecurityProperties;
import com.polyglotmesh.dto.InteroperabilityRequest;
import com.polyglotmesh.dto.InteroperabilityResponse;
import com.polyglotmesh.exception.InteroperabilityException;
import org.graalvm.polyglot.Engine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageInteroperabilityServiceTest {

    private LanguageInteroperabilityService interoperabilityService;
    private Engine engine;

    @BeforeEach
    void setUp() {
        engine = Engine.newBuilder().build();
        GraalVMConfig.PolyglotContextFactory contextFactory =
                new GraalVMConfig.PolyglotContextFactory(engine, new GraalVMSecurityProperties());
        interoperabilityService = new LanguageInteroperabilityService(contextFactory);
    }

    @AfterEach
    void tearDown() {
        engine.close();
    }

    @Test
    void sharesJavaScriptNumberWithPython() {
        InteroperabilityResponse response = interoperabilityService.exchange(new InteroperabilityRequest(
                "javascript", "21 * 2", "python", "sharedValue + 1"));

        assertTrue(response.success());
        assertEquals("js", response.sourceLanguage());
        assertEquals("python", response.targetLanguage());
        assertEquals("number", response.valueType());
        assertEquals("42.0", response.transferredValue());
        assertEquals("43.0", response.targetResult());
    }

    @Test
    void sharesPythonStringWithJavaScript() {
        InteroperabilityResponse response = interoperabilityService.exchange(new InteroperabilityRequest(
                "python", "'hello'", "javascript", "sharedValue + ' world'"));

        assertTrue(response.success());
        assertEquals("string", response.valueType());
        assertEquals("hello", response.transferredValue());
        assertEquals("hello world", response.targetResult());
    }

    @Test
    void rejectsUnsupportedGuestValueTypes() {
        InteroperabilityException exception = assertThrows(
                InteroperabilityException.class,
                () -> interoperabilityService.exchange(new InteroperabilityRequest(
                        "javascript", "true", "python", "sharedValue")));

        assertEquals("UNSUPPORTED_VALUE_TYPE", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Only strings and finite"));
    }

    @Test
    void reportsTargetLanguageExecutionErrors() {
        InteroperabilityException exception = assertThrows(
                InteroperabilityException.class,
                () -> interoperabilityService.exchange(new InteroperabilityRequest(
                        "javascript", "42", "python", "1 / 0")));

        assertEquals("INTEROPERABILITY_EXECUTION_ERROR", exception.getErrorCode());
        assertTrue(exception.getMessage().startsWith("Execution error: "));
    }
}
