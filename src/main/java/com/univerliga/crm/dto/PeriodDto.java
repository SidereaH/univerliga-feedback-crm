package com.univerliga.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "Task period")
public record PeriodDto(
        @NotNull @Schema(example = "2026-01-01") LocalDate from,
        @NotNull @Schema(example = "2026-01-31") LocalDate to
) {
}
