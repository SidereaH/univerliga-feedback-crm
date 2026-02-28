package com.univerliga.crm.service;

import com.univerliga.crm.dto.OutboxEventResponse;
import com.univerliga.crm.dto.PeriodDto;
import com.univerliga.crm.dto.PersonResponse;
import com.univerliga.crm.dto.TaskResponse;
import com.univerliga.crm.model.OutboxEventEntity;
import com.univerliga.crm.model.PersonEntity;
import com.univerliga.crm.model.TaskEntity;
import java.util.ArrayList;
import java.util.Comparator;
import org.springframework.stereotype.Component;

@Component
public class MapperService {

    public PersonResponse toPersonResponse(PersonEntity person, boolean adminView) {
        return new PersonResponse(
                person.getId(),
                person.getDisplayName(),
                person.getEmail(),
                person.getDepartmentId(),
                person.getTeamId(),
                person.getRole(),
                person.isActive(),
                person.getIdentityStatus(),
                adminView ? person.getKeycloakUserId() : null,
                adminView ? person.getLastIdentityError() : null,
                person.getCreatedAt(),
                person.getUpdatedAt()
        );
    }

    public TaskResponse toTaskResponse(TaskEntity task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                new PeriodDto(task.getPeriodFrom(), task.getPeriodTo()),
                task.getOwnerId(),
                task.getAssigneeId(),
                task.getParticipantIds().stream().sorted(Comparator.naturalOrder()).toList(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getClosedAt()
        );
    }

    public OutboxEventResponse toOutboxResponse(OutboxEventEntity event) {
        return new OutboxEventResponse(
                event.getId(),
                event.getType(),
                event.getRoutingKey(),
                event.getStatus(),
                event.getCreatedAt(),
                event.getSentAt()
        );
    }
}
