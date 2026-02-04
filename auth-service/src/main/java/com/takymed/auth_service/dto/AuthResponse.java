package com.takymed.auth_service.dto;

import com.takymed.auth_service.entities.enums.Role;

public record AuthResponse(
        String token,
        String email,
        Role role) {
}
