package com.univerliga.crm.controller;

import com.univerliga.crm.dto.ApiResponse;
import com.univerliga.crm.dto.CreateTaskRequest;
import com.univerliga.crm.dto.PagedResult;
import com.univerliga.crm.dto.TaskClosedResponse;
import com.univerliga.crm.dto.TaskResponse;
import com.univerliga.crm.dto.UpdateTaskRequest;
import com.univerliga.crm.error.ApiExceptionFactory;
import com.univerliga.crm.model.TaskEntity;
import com.univerliga.crm.model.TaskStatus;
import com.univerliga.crm.security.CurrentUser;
import com.univerliga.crm.security.CurrentUserService;
import com.univerliga.crm.service.MapperService;
import com.univerliga.crm.service.TaskService;
import com.univerliga.crm.util.ApiResponseFactory;
import com.univerliga.crm.util.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/crm/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final MapperService mapperService;
    private final CurrentUserService currentUserService;
    private final ApiResponseFactory responseFactory;

    @GetMapping
    @Operation(summary = "Search tasks")
    public ApiResponse<PagedResult<TaskResponse>> search(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) String ownerId,
            @RequestParam(required = false) String assigneeId,
            @RequestParam(required = false) String participantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        CurrentUser user = currentUserService.currentUser();
        ensureTaskReadAccess(user);
        Page<TaskEntity> tasks = taskService.search(
                status, ownerId, assigneeId, participantId, periodFrom, periodTo,
                PageUtils.pageable(page, size), user
        );
        PagedResult<TaskResponse> data = new PagedResult<>(
                tasks.stream().map(mapperService::toTaskResponse).toList(),
                PageUtils.pageMeta(tasks)
        );
        return responseFactory.success(data);
    }

    @PostMapping
    @Operation(summary = "Create task")
    public ApiResponse<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        CurrentUser user = currentUserService.currentUser();
        ensureTaskWriteAccess(user);
        TaskEntity task = taskService.create(request);
        return responseFactory.success(mapperService.toTaskResponse(task));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get task by id")
    public ApiResponse<TaskResponse> getById(@PathVariable String taskId) {
        CurrentUser user = currentUserService.currentUser();
        ensureTaskReadAccess(user);
        TaskEntity task = taskService.getByIdAccessible(taskId, user);
        return responseFactory.success(mapperService.toTaskResponse(task));
    }

    @PatchMapping("/{taskId}")
    @Operation(summary = "Patch task")
    public ApiResponse<TaskResponse> patch(@PathVariable String taskId, @Valid @RequestBody UpdateTaskRequest request) {
        CurrentUser user = currentUserService.currentUser();
        ensureTaskWriteAccess(user);
        TaskEntity task = taskService.patch(taskId, request);
        return responseFactory.success(mapperService.toTaskResponse(task));
    }

    @PostMapping("/{taskId}/close")
    @Operation(summary = "Close task")
    public ApiResponse<TaskClosedResponse> close(@PathVariable String taskId) {
        CurrentUser user = currentUserService.currentUser();
        ensureTaskWriteAccess(user);
        TaskEntity task = taskService.close(taskId);
        return responseFactory.success(new TaskClosedResponse(task.getId(), task.getStatus(), task.getClosedAt(), task.getUpdatedAt()));
    }

    private void ensureTaskReadAccess(CurrentUser user) {
        if (!user.hasAnyRole("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_HR", "ROLE_EMPLOYEE")) {
            throw ApiExceptionFactory.forbidden("No task read access");
        }
    }

    private void ensureTaskWriteAccess(CurrentUser user) {
        if (!user.hasAnyRole("ROLE_ADMIN", "ROLE_MANAGER")) {
            throw ApiExceptionFactory.forbidden("No task write access");
        }
    }
}
