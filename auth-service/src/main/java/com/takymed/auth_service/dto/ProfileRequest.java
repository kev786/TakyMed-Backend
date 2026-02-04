package com.takymed.auth_service.dto;

import jakarta.validation.constraints.NotBlank;

public record ProfileRequest(
        @NotBlank String lastName,
        @NotBlank String firstName,
        String phone,
        String avatarUrl) {
}
