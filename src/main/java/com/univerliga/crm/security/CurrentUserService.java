package com.univerliga.crm.security;

import com.univerliga.crm.error.ApiExceptionFactory;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserService {

    public CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw ApiExceptionFactory.forbidden("Unauthenticated access");
        }

        Set<String> roles = new HashSet<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            roles.add(authority.getAuthority());
        }

        String username = Optional.ofNullable(authentication.getName()).orElse("unknown");
        String personId = claimOrFallback(authentication, "personId", fallbackPersonId(username));
        String departmentId = claimOrFallback(authentication, "departmentId", "d_1");
        String teamId = claimOrFallback(authentication, "teamId", "t_1");

        return new CurrentUser(username, personId, departmentId, teamId, roles);
    }

    private String claimOrFallback(Authentication authentication, String claim, String fallback) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Object value = jwtAuth.getToken().getClaim(claim);
            if (value instanceof String s && !s.isBlank()) {
                return s;
            }
            if (value instanceof List<?> list && !list.isEmpty()) {
                return String.valueOf(list.get(0));
            }
            Object attributesClaim = jwtAuth.getToken().getClaim("attributes");
            if (attributesClaim instanceof Map<?, ?> attrs) {
                Object attrValue = attrs.get(claim);
                if (attrValue instanceof List<?> list && !list.isEmpty()) {
                    return String.valueOf(list.get(0));
                }
            }
        }
        return fallback;
    }

    private String fallbackPersonId(String username) {
        return switch (username) {
            case "employee" -> "p_employee";
            case "manager" -> "p_manager";
            case "admin" -> "p_admin";
            case "hr" -> "p_hr";
            default -> "p_" + username;
        };
    }
}
