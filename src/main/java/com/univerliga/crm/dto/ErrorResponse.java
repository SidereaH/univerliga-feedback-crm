package com.univerliga.crm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error envelope")
public record ErrorResponse(ErrorBody error) {
}
