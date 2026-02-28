package com.univerliga.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Success envelope")
public record ApiResponse<T>(
        T data,
        ApiMeta meta
) {
}
