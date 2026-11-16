package com.polyglotmesh.dto;

public record ExecutionResponse(
        boolean success,
        String language,
        String output,
        String error,
        long executionTimeMs) {
}
