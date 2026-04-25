package com.cism.backend.dto.users;

public record UserDto(
    String email,
    String studentId,
    String username,
    String avatar,
    String password,
    String role
) {
    
}
