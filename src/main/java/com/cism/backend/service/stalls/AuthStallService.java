package com.cism.backend.service.stalls;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cism.backend.config.JwtTokenProvider;
import com.cism.backend.dto.stall.LoginStallDto;
import com.cism.backend.dto.stall.LoginStallResponseDto;
import com.cism.backend.exception.BadrequestException;
import com.cism.backend.model.admin.StallModel;
import com.cism.backend.repository.admin.CreateStallRepository;

import jakarta.transaction.Transactional;

@Service
public class AuthStallService {
    @Autowired
    CreateStallRepository createStallRepository;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Transactional
    public LoginStallResponseDto loginStallService(LoginStallDto entity) {
        String licence = entity.licence();
        String password = entity.password();

        if(isBlank(licence) || isBlank(password)) {
            throw new BadrequestException("All fields are required", "ALL_FIELDS_REQUIRED");
        }
        
        StallModel stall = createStallRepository.findByLicenceAndPassword(licence, password).orElseThrow(() -> new BadrequestException("Invalid credentials", "INVALID_CREDENTIALS"));
    
        String accessToken = jwtTokenProvider.generateToken(stall.getLicence());
        String refreshToken = jwtTokenProvider.generateRefreshToken(stall.getLicence());
        
        return new LoginStallResponseDto(accessToken, refreshToken, stall);
    }


    public boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
