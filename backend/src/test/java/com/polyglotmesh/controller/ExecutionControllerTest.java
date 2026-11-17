package com.polyglotmesh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polyglotmesh.dto.ExecutionRequest;
import com.polyglotmesh.dto.ExecutionResponse;
import com.polyglotmesh.exception.ApiExceptionHandler;
import com.polyglotmesh.service.PolyglotExecutionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExecutionControllerTest {

    private final PolyglotExecutionService executionService = mock(PolyglotExecutionService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ExecutionController(executionService))
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.destroy();
    }

    @Test
    void executeReturnsExistingExecutionResponse() throws Exception {
        ExecutionResponse response = new ExecutionResponse(true, "javascript", "Hello\n", null, 12);
        when(executionService.execute("javascript", "console.log('Hello');")).thenReturn(response);

        mockMvc.perform(post("/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RequestBody("javascript", "console.log('Hello');"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.language").value("javascript"))
                .andExpect(jsonPath("$.output").value("Hello\n"))
                .andExpect(jsonPath("$.executionTimeMs").value(12));

        verify(executionService).execute("javascript", "console.log('Hello');");
    }

    @Test
    void executeReturnsBadRequestForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestBody("ruby", "puts 'Hello'"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value(
                        "Language must be one of: javascript, js, python, py."));
    }

    @Test
    void executeReturnsBadRequestForBlankCode() throws Exception {
        mockMvc.perform(post("/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestBody("python", "  "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Code must not be empty."));
    }

    @Test
    void executeReturnsBadRequestForOversizedCode() throws Exception {
        String code = "x".repeat(ExecutionRequest.MAX_CODE_SIZE + 1);
        mockMvc.perform(post("/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestBody("javascript", code))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Code exceeds the maximum size of 100000 characters."));
    }

    @Test
    void executeReturnsBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Request body must be valid JSON with language and code fields."));
    }

    @Test
    void executionFailureRemainsAnOkHttpResponse() throws Exception {
        ExecutionResponse response =
                new ExecutionResponse(false, "javascript", "", "Syntax error: unexpected token", 2);
        when(executionService.execute("javascript", "const = ;")).thenReturn(response);

        mockMvc.perform(post("/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestBody("javascript", "const = ;"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Syntax error: unexpected token"));
    }

    private record RequestBody(String language, String code) {
    }
}
