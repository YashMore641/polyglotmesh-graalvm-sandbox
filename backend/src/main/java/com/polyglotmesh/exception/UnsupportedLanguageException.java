package com.polyglotmesh.exception;

public class UnsupportedLanguageException extends RuntimeException {
    public UnsupportedLanguageException(String language) {
        super("Unsupported language: " + language
                + ". Supported languages: javascript, js, python, py.");
    }
}
