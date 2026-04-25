package com.cism.backend.dto.users;

import com.cism.backend.model.users.AuthModel;

public record LoginDto(
    String email, 
    String password, 
    String accessToken, 
    String refreshToken, 
    AuthModel user
) {
    
}
