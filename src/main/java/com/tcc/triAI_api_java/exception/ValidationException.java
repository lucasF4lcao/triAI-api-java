package com.tcc.triAI_api_java.exception;

import java.util.List;
import java.util.Map;

public class ValidationException extends RuntimeException {

    private final Map<String, List<String>> errors;

    public ValidationException(Map<String, List<String>> errors) {
        super("Dados inválidos");
        this.errors = errors;
    }

    public static ValidationException of(String field, String message) {
        return new ValidationException(Map.of(field, List.of(message)));
    }

    public Map<String, List<String>> getErrors() {
        return errors;
    }
}
