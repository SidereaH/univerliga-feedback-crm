package com.univerliga.crm.error;

import org.springframework.http.HttpStatus;

public final class ApiExceptionFactory {

    private ApiExceptionFactory() {
    }

    public static ApiException notFound(String message) {
        return new ApiException("NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }

    public static ApiException forbidden(String message) {
        return new ApiException("FORBIDDEN", message, HttpStatus.FORBIDDEN);
    }

    public static ApiException conflict(String message) {
        return new ApiException("CONFLICT", message, HttpStatus.CONFLICT);
    }

    public static ApiException badRequest(String message) {
        return new ApiException("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST);
    }
}
