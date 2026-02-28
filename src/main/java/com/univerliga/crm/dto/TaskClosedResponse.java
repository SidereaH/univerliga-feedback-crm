package com.univerliga.crm.dto;

import com.univerliga.crm.model.TaskStatus;
import java.time.OffsetDateTime;

public record TaskClosedResponse(
        String id,
        TaskStatus status,
        OffsetDateTime closedAt,
        OffsetDateTime updatedAt
) {
}
