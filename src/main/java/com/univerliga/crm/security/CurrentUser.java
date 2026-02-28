package com.univerliga.crm.security;

import java.util.Set;

public record CurrentUser(
        String username,
        String personId,
        String departmentId,
        String teamId,
        Set<String> roles
) {

    public boolean hasAnyRole(String... expected) {
        for (String role : expected) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmployeeOnly() {
        return roles.contains("ROLE_EMPLOYEE")
                && !roles.contains("ROLE_ADMIN")
                && !roles.contains("ROLE_MANAGER")
                && !roles.contains("ROLE_HR");
    }
}
