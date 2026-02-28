package com.univerliga.crm.dto;

import com.univerliga.crm.model.IdentityStatus;
import com.univerliga.crm.model.PersonRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "Person view")
public record PersonResponse(
        @Schema(example = "p_123") String id,
        @Schema(example = "John Smith") String displayName,
        @Schema(example = "john@univerliga.com") String email,
        @Schema(example = "d_1") String departmentId,
        @Schema(example = "t_1") String teamId,
        PersonRole role,
        boolean active,
        IdentityStatus identityStatus,
        @Schema(nullable = true, example = "kc_123") String keycloakUserId,
        @Schema(nullable = true, example = "User already exists") String lastIdentityError,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
