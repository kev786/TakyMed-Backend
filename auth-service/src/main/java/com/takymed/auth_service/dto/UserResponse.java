package com.takymed.auth_service.dto;

import com.takymed.auth_service.entities.enums.Role;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String lastName,
        String firstName,
        Role role,
        String phone,
        String avatarUrl) {
}
