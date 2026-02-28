package com.univerliga.crm.controller;

import com.univerliga.crm.dto.ApiResponse;
import com.univerliga.crm.dto.OutboxEventResponse;
import com.univerliga.crm.dto.PagedResult;
import com.univerliga.crm.dto.ReplayResult;
import com.univerliga.crm.model.OutboxEventEntity;
import com.univerliga.crm.model.OutboxStatus;
import com.univerliga.crm.security.CurrentUser;
import com.univerliga.crm.security.CurrentUserService;
import com.univerliga.crm.security.SecurityRules;
import com.univerliga.crm.service.MapperService;
import com.univerliga.crm.service.OutboxService;
import com.univerliga.crm.util.ApiResponseFactory;
import com.univerliga.crm.util.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/crm/outbox")
@RequiredArgsConstructor
public class OutboxController {

    private final OutboxService outboxService;
    private final MapperService mapperService;
    private final CurrentUserService currentUserService;
    private final SecurityRules securityRules;
    private final ApiResponseFactory responseFactory;

    @GetMapping
    @Operation(summary = "List outbox events")
    public ApiResponse<PagedResult<OutboxEventResponse>> list(
            @RequestParam(required = false) OutboxStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        CurrentUser user = currentUserService.currentUser();
        securityRules.requireAdmin(user);
        Page<OutboxEventEntity> events = outboxService.listByStatus(status, PageUtils.pageable(page, size));
        PagedResult<OutboxEventResponse> data = new PagedResult<>(events.stream().map(mapperService::toOutboxResponse).toList(), PageUtils.pageMeta(events));
        return responseFactory.success(data);
    }

    @PostMapping("/{eventId}/replay")
    @Operation(summary = "Replay outbox event")
    public ApiResponse<ReplayResult> replay(@PathVariable String eventId) {
        CurrentUser user = currentUserService.currentUser();
        securityRules.requireAdmin(user);
        outboxService.replay(eventId);
        return responseFactory.success(new ReplayResult(true));
    }
}
