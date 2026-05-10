package com.cism.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.context.annotation.Lazy;

import com.cism.backend.util.CookieUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    @Lazy
    private UserDetailsService userDetailsService;

    @Autowired
    private CookieUtil cookieUtil;

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                setAuthentication(jwt, request);
            } else {
                attemptAutoRefresh(request, response);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication", ex);
        }

        filterChain.doFilter(request, response);
    }

    private void attemptAutoRefresh(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null)
            return;

        String appType = request.getHeader("X-App-Type");
        if (appType == null)
            appType = request.getParameter("appType");

        boolean isStall = "stall".equalsIgnoreCase(appType) ||
                request.getRequestURI().contains("/stall") ||
                request.getRequestURI().contains("/owner");

        String tokenName = isStall ? "stall_token" : "user_token";
        String refreshName = isStall ? "stall_refresh_token" : "user_refresh_token";

        String refreshToken = null;
        for (Cookie cookie : cookies) {
            if (refreshName.equals(cookie.getName())) {
                refreshToken = cookie.getValue();
                break;
            }
        }

        if (StringUtils.hasText(refreshToken) && tokenProvider.validateToken(refreshToken)) {
            String username = tokenProvider.getUsernameFromJWT(refreshToken);
            String newAccessToken = tokenProvider.generateToken(username);
            cookieUtil.addCookie(response, tokenName, newAccessToken, tokenProvider.getJwtExpirationInMs());
            setAuthentication(newAccessToken, request);
        }
    }

    private void setAuthentication(String jwt, HttpServletRequest request) {
        String username = tokenProvider.getUsernameFromJWT(jwt);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // get jwt from request
    // Authorization: Bearer <token>
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null)
            return null;

        String stallToken = null;
        String userToken = null;

        for (Cookie cookie : cookies) {
            if ("stall_token".equals(cookie.getName()))
                stallToken = cookie.getValue();
            else if ("user_token".equals(cookie.getName()))
                userToken = cookie.getValue();
        }

        String appType = request.getHeader("X-App-Type");
        if (appType == null)
            appType = request.getParameter("appType");

        boolean isStall = "stall".equalsIgnoreCase(appType) ||
                request.getRequestURI().contains("/stall") ||
                request.getRequestURI().contains("/owner");

        if (isStall)
            return stallToken != null ? stallToken : userToken;
        return userToken != null ? userToken : stallToken;
    }
}
