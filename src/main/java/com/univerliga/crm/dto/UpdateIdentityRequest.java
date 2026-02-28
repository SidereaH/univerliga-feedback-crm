package com.univerliga.crm.dto;

import com.univerliga.crm.model.IdentityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Manual identity status update")
public record UpdateIdentityRequest(
        @NotNull IdentityStatus identityStatus,
        @Schema(nullable = true, example = "kc_user_123") String keycloakUserId,
        @Schema(nullable = true, example = "Provisioning timeout") String lastIdentityError
) {
}
