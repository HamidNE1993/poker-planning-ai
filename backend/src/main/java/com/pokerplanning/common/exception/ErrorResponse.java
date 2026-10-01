package com.pokerplanning.common.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
    int status,
    String error,
    String message,
    String path,
    Instant timestamp,
    List<ValidationError> validationErrors
) {
    public record ValidationError(String field, String message) {}

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, Instant.now(), List.of());
    }

    public static ErrorResponse ofValidation(int status, String error, String message, String path, List<ValidationError> errors) {
        return new ErrorResponse(status, error, message, path, Instant.now(), errors);
    }
}
