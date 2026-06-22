package com.microservices.auth.dto;

public record TemporaryPasswordResponse(
        Long userId,
        String email,
        String temporaryPassword
) {
}
