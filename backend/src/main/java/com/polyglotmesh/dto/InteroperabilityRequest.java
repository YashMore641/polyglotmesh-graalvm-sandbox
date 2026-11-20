package com.polyglotmesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InteroperabilityRequest(
        @NotBlank(message = "Source language must not be empty.")
        @Pattern(regexp = "(?i)\\s*(javascript|js|python|py)\\s*",
                message = "Language must be one of: javascript, js, python, py.")
        String sourceLanguage,
        @NotBlank(message = "Source code must not be empty.")
        @Size(max = ExecutionRequest.MAX_CODE_SIZE,
                message = "Code exceeds the maximum size of 100000 characters.")
        String sourceCode,
        @NotBlank(message = "Target language must not be empty.")
        @Pattern(regexp = "(?i)\\s*(javascript|js|python|py)\\s*",
                message = "Language must be one of: javascript, js, python, py.")
        String targetLanguage,
        @NotBlank(message = "Target code must not be empty.")
        @Size(max = ExecutionRequest.MAX_CODE_SIZE,
                message = "Code exceeds the maximum size of 100000 characters.")
        String targetCode) {
}
