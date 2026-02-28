package com.univerliga.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Create task")
public record CreateTaskRequest(
        @NotBlank @Size(min = 1, max = 200) String title,
        @Size(max = 5000) String description,
        @Valid @NotNull PeriodDto period,
        @NotBlank String ownerId,
        String assigneeId,
        List<String> participantIds
) {
}
