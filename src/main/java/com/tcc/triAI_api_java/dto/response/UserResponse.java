package com.tcc.triAI_api_java.dto.response;

import com.tcc.triAI_api_java.entity.User;

public record UserResponse(Long id, String name, String email, boolean active) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.isActive());
    }
}
