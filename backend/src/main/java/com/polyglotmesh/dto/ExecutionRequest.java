package com.polyglotmesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExecutionRequest(
        @NotBlank(message = "Language must not be empty.")
        String language,
        @NotBlank(message = "Code must not be empty.")
        @Size(max = MAX_CODE_SIZE, message = "Code exceeds the maximum size of 100000 characters.")
        String code) {

    public static final int MAX_CODE_SIZE = 100_000;
}
