package com.cism.backend.service.stalls;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cism.backend.config.JwtTokenProvider;
import com.cism.backend.exception.BadrequestException;
import com.cism.backend.util.CookieUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class tokenStallService {
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CookieUtil cookieUtil;

    public void refreshTokenService(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()){
                if ("refresh_token".equals(cookie.getName())){
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadrequestException("Invalid or missing refresh token", "INVALID_REFRESH_TOKEN");
        }

        String licence = jwtTokenProvider.getUsernameFromJWT(refreshToken);
        String newAccessToken = jwtTokenProvider.generateToken(licence);
        cookieUtil.addCookie(response, "access_token", newAccessToken, jwtTokenProvider.getJwtExpirationInMs());        
    }

}
