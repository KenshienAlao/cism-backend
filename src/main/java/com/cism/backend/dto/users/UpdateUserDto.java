package com.cism.backend.dto.users;

public record UpdateUserDto(
        String clientName,
        String studentId,
        String role) {

}
