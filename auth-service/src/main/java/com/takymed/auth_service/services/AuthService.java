package com.takymed.auth_service.services;

import com.takymed.auth_service.dto.AuthResponse;
import com.takymed.auth_service.dto.LoginRequest;
import com.takymed.auth_service.dto.RegisterRequest;

public interface AuthService {
    AuthResponse login(LoginRequest loginRequest);

    AuthResponse register(RegisterRequest registerRequest);
}
