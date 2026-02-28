package com.univerliga.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Error payload")
public record ErrorBody(
        @Schema(example = "VALIDATION_ERROR") String code,
        @Schema(example = "Validation failed") String message,
        List<ErrorDetail> details,
        @Schema(example = "3f57ea99-72ed-49d8-baf5-3f71ff2090c4") String requestId
) {
}
