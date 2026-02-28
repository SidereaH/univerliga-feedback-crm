package com.univerliga.crm.controller;

import com.univerliga.crm.dto.ApiResponse;
import com.univerliga.crm.dto.SystemVersionResponse;
import com.univerliga.crm.util.ApiResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemController {

    private final ApiResponseFactory responseFactory;

    @Value("${spring.application.name:univerliga-crm-service}")
    private String appName;

    @Value("${app.service-version:0.1.0}")
    private String version;

    @GetMapping("/version")
    @Operation(summary = "Get service version")
    public ApiResponse<SystemVersionResponse> version() {
        return responseFactory.success(new SystemVersionResponse(appName, version));
    }
}
