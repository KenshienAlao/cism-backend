package com.cism.backend.dto.users;

public record RegisterDto(
        String username,
        String studentId,
        String email,
        String password,
        String otp
) {
        
}
