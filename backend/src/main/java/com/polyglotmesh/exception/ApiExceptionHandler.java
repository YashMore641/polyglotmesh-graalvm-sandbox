package com.polyglotmesh.exception;

import com.polyglotmesh.dto.ExecutionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.bind.support.WebExchangeBindException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExecutionResponse> handleInvalidRequest(MethodArgumentNotValidException exception) {
        return badRequest(validationError(exception.getBindingResult()));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ExecutionResponse> handleInvalidWebFluxRequest(WebExchangeBindException exception) {
        return badRequest(validationError(exception.getBindingResult()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ExecutionResponse> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        return badRequest("Request body must be valid JSON with language and code fields.");
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ExecutionResponse> handleInvalidWebFluxInput(ServerWebInputException exception) {
        return badRequest("Request body must be valid JSON with language and code fields.");
    }

    private String validationError(BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
                .findFirst()
                .map(ObjectError::getDefaultMessage)
                .orElse("Request validation failed.");
    }

    private ResponseEntity<ExecutionResponse> badRequest(String error) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ExecutionResponse(false, null, "", error, 0));
    }
}
