package com.takymed.auth_service.exceptions;

import org.springframework.http.HttpStatus;

public class AuthException extends AppException {
    public AuthException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
