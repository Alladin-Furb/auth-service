package com.microservices.auth.dto;

import com.microservices.auth.entity.Role;
import com.microservices.auth.entity.User;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        Role role,
        UUID profileId
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getProfileId()
        );
    }
}
