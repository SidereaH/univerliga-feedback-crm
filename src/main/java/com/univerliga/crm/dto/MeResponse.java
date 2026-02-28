package com.univerliga.crm.dto;

import java.util.List;

public record MeResponse(
        String personId,
        String username,
        List<String> roles,
        String departmentId,
        String teamId,
        String displayName
) {
}
