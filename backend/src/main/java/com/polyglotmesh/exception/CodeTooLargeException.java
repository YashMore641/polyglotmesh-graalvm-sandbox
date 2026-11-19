package com.polyglotmesh.exception;

public class CodeTooLargeException extends RuntimeException {
    public CodeTooLargeException(int maxSize) {
        super("Code exceeds the maximum size of " + maxSize + " characters.");
    }
}
