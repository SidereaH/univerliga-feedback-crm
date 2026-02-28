package com.univerliga.crm.controller;

import com.univerliga.crm.dto.ApiResponse;
import com.univerliga.crm.dto.CreatePersonRequest;
import com.univerliga.crm.dto.DeleteResult;
import com.univerliga.crm.dto.PagedResult;
import com.univerliga.crm.dto.PersonResponse;
import com.univerliga.crm.dto.UpdateIdentityRequest;
import com.univerliga.crm.dto.UpdatePersonRequest;
import com.univerliga.crm.error.ApiExceptionFactory;
import com.univerliga.crm.model.PersonEntity;
import com.univerliga.crm.security.CurrentUser;
import com.univerliga.crm.security.CurrentUserService;
import com.univerliga.crm.security.SecurityRules;
import com.univerliga.crm.service.MapperService;
import com.univerliga.crm.service.PersonService;
import com.univerliga.crm.util.ApiResponseFactory;
import com.univerliga.crm.util.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/crm/people")
@RequiredArgsConstructor
public class PeopleController {

    private final PersonService personService;
    private final MapperService mapperService;
    private final ApiResponseFactory responseFactory;
    private final CurrentUserService currentUserService;
    private final SecurityRules securityRules;

    @GetMapping
    @Operation(summary = "Search people")
    public ApiResponse<PagedResult<PersonResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String teamId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        CurrentUser user = currentUserService.currentUser();
        securityRules.requirePeopleReadAccess(user);
        Page<PersonEntity> result = personService.search(query, departmentId, teamId, active, PageUtils.pageable(page, size));
        PagedResult<PersonResponse> data = new PagedResult<>(
                result.stream().map(p -> mapperService.toPersonResponse(p, user.hasAnyRole("ROLE_ADMIN"))).toList(),
                PageUtils.pageMeta(result)
        );
        return responseFactory.success(data);
    }

    @PostMapping
    @Operation(summary = "Create person")
    public ApiResponse<PersonResponse> create(@Valid @RequestBody CreatePersonRequest request) {
        CurrentUser user = currentUserService.currentUser();
        securityRules.requireAdmin(user);
        PersonEntity created = personService.create(request);
        return responseFactory.success(mapperService.toPersonResponse(created, true));
    }

    @GetMapping("/{personId}")
    @Operation(summary = "Get person by id")
    public ApiResponse<PersonResponse> getById(@PathVariable String personId) {
        CurrentUser user = currentUserService.currentUser();
        if (user.isEmployeeOnly() && !user.personId().equals(personId)) {
            throw ApiExceptionFactory.forbidden("Employee can only access self person");
        }
        if (!user.isEmployeeOnly() && !user.hasAnyRole("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_HR")) {
            throw ApiExceptionFactory.forbidden("No access");
        }
        PersonEntity person = personService.getById(personId);
        return responseFactory.success(mapperService.toPersonResponse(person, user.hasAnyRole("ROLE_ADMIN")));
    }

    @PatchMapping("/{personId}")
    @Operation(summary = "Patch person")
    public ApiResponse<PersonResponse> patch(@PathVariable String personId, @Valid @RequestBody UpdatePersonRequest request) {
        CurrentUser user = currentUserService.currentUser();
        securityRules.requireAdmin(user);
        PersonEntity person = personService.patch(personId, request);
        return responseFactory.success(mapperService.toPersonResponse(person, true));
    }

    @DeleteMapping("/{personId}")
    @Operation(summary = "Soft delete person")
    public ApiResponse<DeleteResult> delete(@PathVariable String personId) {
        CurrentUser user = currentUserService.currentUser();
        securityRules.requireAdmin(user);
        personService.softDelete(personId);
        return responseFactory.success(new DeleteResult(true));
    }

    @PostMapping("/{personId}/identity")
    @Operation(summary = "Manual identity status update")
    public ApiResponse<PersonResponse> updateIdentity(@PathVariable String personId, @Valid @RequestBody UpdateIdentityRequest request) {
        CurrentUser user = currentUserService.currentUser();
        securityRules.requireAdmin(user);
        PersonEntity person = personService.updateIdentity(personId, request);
        return responseFactory.success(mapperService.toPersonResponse(person, true));
    }
}
