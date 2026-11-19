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
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(
                        "Language must be one of: javascript, js, python, py."));
    }

    @Test
    void executeReturnsBadRequestForBlankCode() throws Exception {
        mockMvc.perform(post("/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestBody("python", "  "))))
                .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                        .andExpect(jsonPath("$.message").value("Code must not be empty."));
    }

    @Test
    void executeReturnsBadRequestForOversizedCode() throws Exception {
        String code = "x".repeat(ExecutionRequest.MAX_CODE_SIZE + 1);
        mockMvc.perform(post("/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestBody("javascript", code))))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error").value("CODE_TOO_LARGE"))
                .andExpect(jsonPath("$.message").value(
                        "Code exceeds the maximum size of 100000 characters."));
    }

    @Test
    void executeReturnsBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value(
                        "Request body must be valid JSON with language and code fields."));
    }

    @Test
    void executionSyntaxFailureReturnsUnprocessableEntity() throws Exception {
        ExecutionResponse response =
                new ExecutionResponse(false, "javascript", "", "Syntax error: unexpected token", 2);
        when(executionService.execute("javascript", "const = ;")).thenReturn(response);

        mockMvc.perform(post("/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestBody("javascript", "const = ;"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("SYNTAX_ERROR"))
                .andExpect(jsonPath("$.message").value("Syntax error: unexpected token"));
    }

    @Test
    void executionTimeoutReturnsGatewayTimeout() throws Exception {
        ExecutionResponse response =
                new ExecutionResponse(false, "javascript", "", "Execution timeout: limit reached", 30000);
        when(executionService.execute("javascript", "while(true){}")).thenReturn(response);

        mockMvc.perform(post("/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestBody("javascript", "while(true){}"))))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.error").value("EXECUTION_TIMEOUT"))
                .andExpect(jsonPath("$.message").value("Execution timeout: limit reached"));
    }

    @Test
    void runtimeFailureReturnsUnprocessableEntity() throws Exception {
        ExecutionResponse response =
                new ExecutionResponse(false, "python", "", "Execution error: division by zero", 2);
        when(executionService.execute("python", "1 / 0")).thenReturn(response);

        mockMvc.perform(post("/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestBody("python", "1 / 0"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("EXECUTION_ERROR"))
                .andExpect(jsonPath("$.message").value("Execution error: division by zero"));
    }

    private record RequestBody(String language, String code) {
    }
}
