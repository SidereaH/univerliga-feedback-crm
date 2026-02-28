package com.univerliga.crm.dto;

import com.univerliga.crm.model.OutboxStatus;
import java.time.OffsetDateTime;

public record OutboxEventResponse(
        String id,
        String type,
        String routingKey,
        OutboxStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime sentAt
) {
}
