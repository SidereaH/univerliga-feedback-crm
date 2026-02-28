package com.univerliga.crm.controller;

import com.univerliga.crm.dto.ApiResponse;
import com.univerliga.crm.dto.MeResponse;
import com.univerliga.crm.model.PersonEntity;
import com.univerliga.crm.security.CurrentUser;
import com.univerliga.crm.security.CurrentUserService;
import com.univerliga.crm.service.PersonService;
import com.univerliga.crm.util.ApiResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MeController {

    private final CurrentUserService currentUserService;
    private final PersonService personService;
    private final ApiResponseFactory responseFactory;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ApiResponse<MeResponse> me() {
        CurrentUser user = currentUserService.currentUser();
        String displayName;
        try {
            PersonEntity person = personService.getById(user.personId());
            displayName = person.getDisplayName();
        } catch (Exception ignored) {
            displayName = user.username();
        }

        return responseFactory.success(new MeResponse(
                user.personId(),
                user.username(),
                user.roles().stream().sorted().toList(),
                user.departmentId(),
                user.teamId(),
                displayName
        ));
    }
}
