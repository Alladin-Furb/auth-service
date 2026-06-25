package com.microservices.auth.dto;

import java.util.UUID;

public record TemporaryPasswordResponse(
        UUID userId,
        String email,
        String temporaryPassword
) {
}
