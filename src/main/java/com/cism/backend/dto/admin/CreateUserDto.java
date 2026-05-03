package com.cism.backend.dto.admin;

import org.springframework.web.multipart.MultipartFile;


public record CreateUserDto(
    String name,
    String description,
    MultipartFile image,
    String openAt,
    String closeAt,
    String role
) {
    
}
