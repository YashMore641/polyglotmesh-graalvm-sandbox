package com.polyglotmesh.dto;

public record PythonExecutionResult(
        boolean success,
        String result,
        String output,
        String error) {
}
