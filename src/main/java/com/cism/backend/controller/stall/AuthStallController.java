package com.cism.backend.controller.stall;

import com.cism.backend.util.CookieUtil;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cism.backend.config.JwtTokenProvider;
import com.cism.backend.dto.common.Api;
import com.cism.backend.dto.stall.LoginStallDto;
import com.cism.backend.dto.stall.LoginStallResponseDto;
import com.cism.backend.service.stalls.AuthStallService;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/api/stall")
public class AuthStallController {
    
    private final CookieUtil cookieUtil;
    private final AuthStallService authStallService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthStallController(AuthStallService authStallService, CookieUtil cookieUtil, JwtTokenProvider jwtTokenProvider) {
        this.authStallService = authStallService;
        this.cookieUtil = cookieUtil;
        this.jwtTokenProvider = jwtTokenProvider;
    }



    @GetMapping("/test")
    public String test() {
        return "Hello world";
    }
    
    @PostMapping("/login")
    public ResponseEntity<Api<LoginStallResponseDto>> loginStall(@RequestBody LoginStallDto entity, HttpServletResponse response) throws Exception {

        LoginStallResponseDto success = authStallService.loginStallService(entity);


        cookieUtil.addCookie(response, "access_token", success.accessToken(), jwtTokenProvider.getJwtExpirationInMs());
        cookieUtil.addCookie(response, "refresh_token", success.refreshToken(), jwtTokenProvider.getRefreshTokenExpirationInMs());

        return ResponseEntity.ok(Api.ok("Login Success", "LOGIN_SUCCESS", success));
    }

    
    

}
