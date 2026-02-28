package com.univerliga.crm.dto;

import com.univerliga.crm.model.TaskStatus;
import java.time.OffsetDateTime;
import java.util.List;

public record TaskResponse(
        String id,
        String title,
        String description,
        TaskStatus status,
        PeriodDto period,
        String ownerId,
        String assigneeId,
        List<String> participantIds,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime closedAt
) {
}
