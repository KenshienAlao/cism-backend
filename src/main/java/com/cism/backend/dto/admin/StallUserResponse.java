package com.cism.backend.dto.admin;

public record StallUserResponse(
        Long id,
        Long stallId,
        String name,
        String description,
        String image,
        String openAt,
        String closeAt,
        Boolean status,
        String role) {

}
