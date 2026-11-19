package com.polyglotmesh.controller;

import com.polyglotmesh.dto.ExecutionRequest;
import com.polyglotmesh.dto.ExecutionResponse;
import com.polyglotmesh.dto.ApiErrorResponse;
import com.polyglotmesh.service.PolyglotExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class ExecutionController {

    private final PolyglotExecutionService polyglotExecutionService;

    public ExecutionController(PolyglotExecutionService polyglotExecutionService) {
        this.polyglotExecutionService = polyglotExecutionService;
    }

    @PostMapping("/execute")
    public ResponseEntity<?> execute(
            @Valid @RequestBody ExecutionRequest request) {
        ExecutionResponse response = polyglotExecutionService.execute(request.language(), request.code());
        if (!response.success()) {
            return executionFailure(response);
        }
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<ApiErrorResponse> executionFailure(
            ExecutionResponse response) {
        String message = response.error() == null ? "Code execution failed." : response.error();
        HttpStatus status;
        String errorCode;
        if (message.startsWith("Execution timeout:")) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            errorCode = "EXECUTION_TIMEOUT";
        } else if (message.startsWith("Syntax error:")
                || message.startsWith("Execution error:")
                || message.startsWith("Execution limit exceeded:")
                || message.startsWith("Security restriction:")) {
            status = HttpStatus.UNPROCESSABLE_ENTITY;
            errorCode = message.startsWith("Syntax error:")
                    ? "SYNTAX_ERROR"
                    : message.startsWith("Execution timeout:")
                            ? "EXECUTION_TIMEOUT"
                            : "EXECUTION_ERROR";
        } else if (message.startsWith("Unsupported language:")) {
            status = HttpStatus.BAD_REQUEST;
            errorCode = "UNSUPPORTED_LANGUAGE";
        } else if (message.contains("maximum size")) {
            status = HttpStatus.PAYLOAD_TOO_LARGE;
            errorCode = "CODE_TOO_LARGE";
        } else {
            status = HttpStatus.UNPROCESSABLE_ENTITY;
            errorCode = "EXECUTION_ERROR";
        }
        ApiErrorResponse error = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                errorCode,
                message,
                "/api/execute");
        return ResponseEntity.status(status).body(error);
    }
}
