package com.polyglotmesh.dto;

public record InteroperabilityResponse(
        boolean success,
        String sourceLanguage,
        String targetLanguage,
        String valueType,
        String transferredValue,
        String targetResult,
        long executionTimeMs) {
}
