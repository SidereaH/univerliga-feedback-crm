package com.univerliga.crm.dto;

import com.univerliga.crm.model.PersonRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Create person")
public record CreatePersonRequest(
        @NotBlank @Size(min = 1, max = 200) @Schema(example = "John Smith") String displayName,
        @NotBlank @Email @Schema(example = "john@univerliga.com") String email,
        @NotBlank @Schema(example = "d_1") String departmentId,
        @NotBlank @Schema(example = "t_1") String teamId,
        @NotNull PersonRole role
) {
}
