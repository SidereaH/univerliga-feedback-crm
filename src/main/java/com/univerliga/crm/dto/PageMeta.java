package com.univerliga.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Page metadata")
public record PageMeta(
        @Schema(example = "1") int page,
        @Schema(example = "20") int size,
        @Schema(example = "100") long totalItems,
        @Schema(example = "5") int totalPages
) {
}
