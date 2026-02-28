package com.univerliga.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error detail")
public record ErrorDetail(
        @Schema(example = "email") String field,
        @Schema(example = "must be a valid email") String issue
) {
}
