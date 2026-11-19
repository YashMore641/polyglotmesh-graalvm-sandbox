package com.polyglotmesh.exception;

import com.polyglotmesh.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(
            MethodArgumentNotValidException exception) {
        return validationError(exception.getBindingResult());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidWebFluxRequest(
            WebExchangeBindException exception) {
        return validationError(exception.getBindingResult());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(
            HttpMessageNotReadableException exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "Request body must be valid JSON with language and code fields.");
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidWebFluxInput(
            ServerWebInputException exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "Request body must be valid JSON with language and code fields.");
    }

    @ExceptionHandler(UnsupportedLanguageException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedLanguage(
            UnsupportedLanguageException exception) {
        return error(HttpStatus.BAD_REQUEST, "UNSUPPORTED_LANGUAGE", exception.getMessage());
    }

    @ExceptionHandler(CodeTooLargeException.class)
    public ResponseEntity<ApiErrorResponse> handleCodeTooLarge(
            CodeTooLargeException exception) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "CODE_TOO_LARGE", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred.");
    }

    private ResponseEntity<ApiErrorResponse> validationError(
            BindingResult bindingResult) {
        String message = bindingResult.getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Request validation failed.");
        HttpStatus status = message.contains("maximum size")
                ? HttpStatus.PAYLOAD_TOO_LARGE
                : HttpStatus.BAD_REQUEST;
        String errorCode = status == HttpStatus.PAYLOAD_TOO_LARGE ? "CODE_TOO_LARGE" : "INVALID_REQUEST";
        return error(status, errorCode, message);
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String error,
            String message) {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                error,
                message,
                "/api/execute");
        return ResponseEntity.status(status).body(response);
    }
}
