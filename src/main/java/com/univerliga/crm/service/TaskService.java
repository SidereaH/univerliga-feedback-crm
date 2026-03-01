package com.univerliga.crm.service;

import com.univerliga.crm.dto.CreateTaskRequest;
import com.univerliga.crm.dto.UpdateTaskRequest;
import com.univerliga.crm.error.ApiExceptionFactory;
import com.univerliga.crm.model.TaskEntity;
import com.univerliga.crm.model.TaskStatus;
import com.univerliga.crm.repository.TaskRepository;
import com.univerliga.crm.security.CurrentUser;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final PersonService personService;
    private final OutboxService outboxService;

    @Transactional(readOnly = true)
    public Page<TaskEntity> search(TaskStatus status,
                                   String ownerId,
                                   String assigneeId,
                                   String participantId,
                                   LocalDate periodFrom,
                                   LocalDate periodTo,
                                   Pageable pageable,
                                   CurrentUser user) {
        Page<TaskEntity> result;
        if (user.isEmployeeOnly()) {
            result = taskRepository.searchForEmployee(user.personId(), pageable);
        } else {
            result = taskRepository.search(status, ownerId, assigneeId, participantId, periodFrom, periodTo, pageable);
        }
        result.forEach(this::initializeParticipants);
        return result;
    }

    @Transactional(readOnly = true)
    public TaskEntity getById(String taskId) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> ApiExceptionFactory.notFound("Task not found: " + taskId));
        initializeParticipants(task);
        return task;
    }

    @Transactional(readOnly = true)
    public TaskEntity getByIdAccessible(String taskId, CurrentUser user) {
        TaskEntity task = getById(taskId);
        if (user.isEmployeeOnly() && !taskRepository.hasAccess(taskId, user.personId())) {
            throw ApiExceptionFactory.forbidden("No access to task");
        }
        return task;
    }

    @Transactional
    public TaskEntity create(CreateTaskRequest request) {
        validatePeriod(request.period().from(), request.period().to());

        personService.ensureActivePerson(request.ownerId());
        if (request.assigneeId() != null && !request.assigneeId().isBlank()) {
            personService.ensureActivePerson(request.assigneeId());
        }
        Set<String> participants = normalizeParticipants(request.participantIds());
        participants.forEach(personService::ensureActivePerson);

        TaskEntity task = new TaskEntity();
        task.setId("task_" + UUID.randomUUID());
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(TaskStatus.DRAFT);
        task.setPeriodFrom(request.period().from());
        task.setPeriodTo(request.period().to());
        task.setOwnerId(request.ownerId());
        task.setAssigneeId(blankToNull(request.assigneeId()));
        task.setParticipantIds(participants);

        taskRepository.save(task);
        outboxService.enqueue("Task", task.getId(), "TaskCreated", "crm.task.created", toTaskEventPayload(task));
        return task;
    }

    @Transactional
    public TaskEntity patch(String taskId, UpdateTaskRequest request) {
        TaskEntity task = getById(taskId);

        if (request.title() != null) {
            task.setTitle(request.title());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.period() != null) {
            validatePeriod(request.period().from(), request.period().to());
            task.setPeriodFrom(request.period().from());
            task.setPeriodTo(request.period().to());
        }
        if (request.status() != null) {
            if (request.status() == TaskStatus.CLOSED) {
                throw ApiExceptionFactory.badRequest("Use close endpoint to close task");
            }
            task.setStatus(request.status());
        }
        if (request.assigneeId() != null) {
            String assigneeId = blankToNull(request.assigneeId());
            if (assigneeId != null) {
                personService.ensureActivePerson(assigneeId);
            }
            task.setAssigneeId(assigneeId);
        }
        if (request.participantIds() != null) {
            Set<String> participants = normalizeParticipants(request.participantIds());
            participants.forEach(personService::ensureActivePerson);
            task.setParticipantIds(participants);
        }

        taskRepository.save(task);
        outboxService.enqueue("Task", task.getId(), "TaskUpdated", "crm.task.updated", toTaskEventPayload(task));
        return task;
    }

    @Transactional
    public TaskEntity close(String taskId) {
        TaskEntity task = getById(taskId);
        task.setStatus(TaskStatus.CLOSED);
        task.setClosedAt(OffsetDateTime.now());
        taskRepository.save(task);
        outboxService.enqueue("Task", task.getId(), "TaskClosed", "crm.task.closed", toTaskEventPayload(task));
        return task;
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw ApiExceptionFactory.badRequest("Invalid period: from must be <= to");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private Set<String> normalizeParticipants(java.util.List<String> input) {
        if (input == null) {
            return new HashSet<>();
        }
        return new HashSet<>(input.stream().filter(v -> v != null && !v.isBlank()).toList());
    }

    private Map<String, Object> toTaskEventPayload(TaskEntity task) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", task.getId());
        payload.put("title", task.getTitle());
        payload.put("status", task.getStatus().name());
        payload.put("period", Map.of("from", task.getPeriodFrom().toString(), "to", task.getPeriodTo().toString()));
        payload.put("ownerId", task.getOwnerId());
        payload.put("assigneeId", task.getAssigneeId());
        payload.put("participantIds", task.getParticipantIds());
        payload.put("updatedAt", task.getUpdatedAt() == null ? OffsetDateTime.now().toString() : task.getUpdatedAt().toString());
        return payload;
    }

    private void initializeParticipants(TaskEntity task) {
        task.getParticipantIds().size();
    }
}
