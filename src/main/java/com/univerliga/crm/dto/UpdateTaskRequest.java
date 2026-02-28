package com.univerliga.crm.dto;

import com.univerliga.crm.model.TaskStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateTaskRequest(
        @Size(min = 1, max = 200) String title,
        @Size(max = 5000) String description,
        TaskStatus status,
        @Valid PeriodDto period,
        String assigneeId,
        List<String> participantIds
) {
}
