package com.univerliga.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response metadata")
public record ApiMeta(
        @Schema(example = "3f57ea99-72ed-49d8-baf5-3f71ff2090c4") String requestId,
        @Schema(example = "2026-02-28T12:00:00Z") String timestamp,
        @Schema(example = "v1") String version
) {
}
