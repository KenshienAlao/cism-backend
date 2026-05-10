package com.cism.backend.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.cism.backend.exception.UnauthorizedException;

import com.cism.backend.model.admin.StallModel;
import com.cism.backend.model.users.AuthModel;
import com.cism.backend.repository.admin.CreateStallRepository;
import com.cism.backend.repository.users.RegisterRepository;

@Component
public class CurrentUserLicence {

    @Autowired
    private RegisterRepository registerRepository;

    @Autowired
    private CreateStallRepository createStallRepository;

    public String getCurrentUserLicence() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("Stall owner not authenticated", "STALL_OWNER_NOT_AUTHENTICATED");
        }
        return auth.getName();
    }

    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("Client not authenticated", "CLIENT_NOT_AUTHENTICATED");
        }
        return auth.getName();
    }

    public AuthModel getCurrentUser() {
        return registerRepository.findByEmail(getCurrentUserEmail())
                .orElseThrow(() -> new UnauthorizedException("User not found", "USER_NOT_FOUND"));
    }

    public StallModel getStall() {
        String licence = getCurrentUserLicence();
        return createStallRepository.findByLicence(licence)
                .orElseThrow(() -> new UnauthorizedException("Stall not found", "STALL_NOT_FOUND"));
    }
}
