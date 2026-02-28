package com.univerliga.crm.dto;

import com.univerliga.crm.model.PersonRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Patch person")
public record UpdatePersonRequest(
        @Size(min = 1, max = 200) @Schema(example = "John Smith") String displayName,
        @Schema(example = "d_2") String departmentId,
        @Schema(example = "t_3") String teamId,
        PersonRole role,
        Boolean active
) {
}
