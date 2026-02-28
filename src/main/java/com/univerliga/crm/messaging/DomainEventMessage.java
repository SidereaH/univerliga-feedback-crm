package com.univerliga.crm.messaging;

import com.fasterxml.jackson.annotation.JsonRawValue;
import java.time.OffsetDateTime;

public record DomainEventMessage(
        String eventId,
        String type,
        OffsetDateTime occurredAt,
        String source,
        @JsonRawValue String payload
) {
}
