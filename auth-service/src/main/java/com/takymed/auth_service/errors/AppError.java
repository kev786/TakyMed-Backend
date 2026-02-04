package com.takymed.auth_service.errors;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AppError(
        LocalDateTime timestamp,
        int status,
        String message,
        String path,
        Map<String, String> errors) {
    public AppError(int status, String message, String path) {
        this(LocalDateTime.now(), status, message, path, null);
    }

    public AppError(int status, String message, String path, Map<String, String> errors) {
        this(LocalDateTime.now(), status, message, path, errors);
    }
}
