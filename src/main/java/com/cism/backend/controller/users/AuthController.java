package com.cism.backend.controller.users;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cism.backend.dto.common.Api;
import com.cism.backend.dto.users.LoginDto;
import com.cism.backend.dto.users.RegisterDto;
import com.cism.backend.model.users.AuthModel;
import com.cism.backend.service.users.AuthService;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import com.cism.backend.util.CookieUtil;
import com.cism.backend.config.JwtTokenProvider;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;



@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final JwtTokenProvider tokenProvider;

    public AuthController(AuthService authService, CookieUtil cookieUtil, JwtTokenProvider tokenProvider){
        this.authService = authService;
        this.cookieUtil = cookieUtil;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<Api<RegisterDto>> register(@RequestBody RegisterDto entity) throws Exception {  
        RegisterDto success = authService.registerService(entity);
        return ResponseEntity.ok(Api.ok("User registered successfully", "USER_REGISTERED", success));
    }

    @PostMapping("/login")
    public ResponseEntity<Api<LoginDto>> login(@RequestBody LoginDto entity, HttpServletResponse response) throws Exception {
        LoginDto success = authService.loginService(entity);
        
        cookieUtil.addCookie(response, "access_token", success.accessToken(), tokenProvider.getJwtExpirationInMs());
        cookieUtil.addCookie(response, "refresh_token", success.refreshToken(), tokenProvider.getRefreshTokenExpirationInMs());

        return ResponseEntity.ok(Api.ok("Login success", "LOGIN_SUCCESS", success));
    }

    @PostMapping("/logout")
    public ResponseEntity<Api<String>> logout(HttpServletResponse response) {
        cookieUtil.clearCookie(response, "access_token");
        cookieUtil.clearCookie(response, "refresh_token");
        return ResponseEntity.ok(Api.ok("Logged out successfully", "LOGOUT_SUCCESS", null));
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<Api<String>> deleteAccount(HttpServletResponse response) {
        authService.deleteAccountService();
        cookieUtil.clearCookie(response, "access_token");
        cookieUtil.clearCookie(response, "refresh_token");
        return ResponseEntity.ok(Api.ok("Account deleted", "ACCOUNT_DELETED", null));
    }

    @GetMapping("/validate-cookie")
    public ResponseEntity<Api<LoginDto>> validateCookie() {
        AuthModel user = authService.validateCookieService();
        if (user == null) {
            return ResponseEntity.ok(Api.error("Not authenticated", "UNAUTHENTICATED", null));
        }
        LoginDto response = new LoginDto(user.getEmail(), null, null, null, user);
        return ResponseEntity.ok(Api.ok("Cookie is valid", "COOKIE_VALID", response));
    }

    @PatchMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity <Api<String>> updateAvatar(@RequestParam("file") MultipartFile file) throws IOException {
        String avatarUrl = authService.updateAvatarService(file);
        return ResponseEntity.ok(Api.ok("Avatar updated", "AVATAR_UPDATED", avatarUrl));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Api<LoginDto>> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        LoginDto success = authService.refreshAccessTokenService(refreshToken);

        cookieUtil.addCookie(response, "access_token", success.accessToken(), tokenProvider.getJwtExpirationInMs());
        cookieUtil.addCookie(response, "refresh_token", success.refreshToken(), tokenProvider.getRefreshTokenExpirationInMs());

        return ResponseEntity.ok(Api.ok("Token refreshed", "TOKEN_REFRESH_SUCCESS", success));
    }
 }
