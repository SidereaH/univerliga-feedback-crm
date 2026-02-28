package com.univerliga.crm.security;

import com.univerliga.crm.error.ApiExceptionFactory;
import org.springframework.stereotype.Component;

@Component
public class SecurityRules {

    public void requireAdmin(CurrentUser user) {
        if (!user.hasAnyRole("ROLE_ADMIN")) {
            throw ApiExceptionFactory.forbidden("Admin role is required");
        }
    }

    public void requireManagerOrAdmin(CurrentUser user) {
        if (!user.hasAnyRole("ROLE_ADMIN", "ROLE_MANAGER")) {
            throw ApiExceptionFactory.forbidden("Manager or Admin role is required");
        }
    }

    public void requirePeopleReadAccess(CurrentUser user) {
        if (!user.hasAnyRole("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_HR")) {
            throw ApiExceptionFactory.forbidden("Role has no people read access");
        }
    }
}
