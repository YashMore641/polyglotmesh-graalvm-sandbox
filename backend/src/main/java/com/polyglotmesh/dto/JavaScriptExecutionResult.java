package com.polyglotmesh.dto;

public record JavaScriptExecutionResult(
        boolean success,
        String result,
        String output,
        String error) {
}
