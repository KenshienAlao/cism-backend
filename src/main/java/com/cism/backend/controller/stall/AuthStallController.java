package com.cism.backend.controller.stall;

import com.cism.backend.util.CookieUtil;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cism.backend.config.JwtTokenProvider;
import com.cism.backend.dto.common.Api;
import com.cism.backend.dto.stall.LoginStallDto;
import com.cism.backend.dto.stall.LoginStallResponseDto;
import com.cism.backend.dto.stall.OwnerStallDto;
import com.cism.backend.service.stalls.AuthStallService;
import com.cism.backend.service.stalls.ProfileStallService;
import com.cism.backend.service.stalls.tokenStallService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/auth/stall")
public class AuthStallController {

    @Autowired
    private CookieUtil cookieUtil;

    @Autowired
    private AuthStallService authStallService;

    @Autowired
    private ProfileStallService profileStallService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private tokenStallService tokenStallService;

    @PostMapping("/login")
    public ResponseEntity<Api<LoginStallResponseDto>> loginStall(@RequestBody LoginStallDto entity,
            HttpServletResponse response) throws Exception {

        LoginStallResponseDto success = authStallService.loginStallService(entity);

        cookieUtil.addCookie(response, "stall_token", success.accessToken(), jwtTokenProvider.getJwtExpirationInMs());
        cookieUtil.addCookie(response, "stall_refresh_token", success.refreshToken(),
                jwtTokenProvider.getRefreshTokenExpirationInMs());

        return ResponseEntity.ok(Api.ok("Login Success", "LOGIN_SUCCESS", success));
    }

    @PostMapping("/logout")
    public ResponseEntity<Api<String>> logout(HttpServletResponse response) {
        cookieUtil.clearCookie(response, "stall_token");
        cookieUtil.clearCookie(response, "stall_refresh_token");
        return ResponseEntity.ok(Api.ok("Logged out successfully", "LOGOUT_SUCCESS", null));
    }

    @GetMapping("/get-profile")
    public ResponseEntity<Api<OwnerStallDto>> getUser() {
        OwnerStallDto data = profileStallService.getUserService();
        return ResponseEntity.ok(Api.ok("Get owner success", "GET_OWNER_SUCCESS", data));
    }

    @GetMapping("/validate-cookie")
    public ResponseEntity<Api<String>> validateCookie() {
        if (!authStallService.validateCookieService()) {
            return ResponseEntity.ok(Api.ok("Cookie is not valid", "COOKIE_NOT_VALID", null));
        }

        return ResponseEntity.ok(Api.ok("Cookie is valid", "COOKIE_VALID", null));
    }

    @GetMapping("/refresh-token")
    public ResponseEntity<Api<String>> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        tokenStallService.refreshTokenService(request, response);
        return ResponseEntity.ok(Api.ok("token", "TOKEN_OK", null));
    }

}
