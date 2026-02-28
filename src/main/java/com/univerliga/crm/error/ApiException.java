package com.univerliga.crm.error;

import java.util.List;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final List<FieldIssue> details;

    public ApiException(String code, String message, HttpStatus status) {
        this(code, message, status, List.of());
    }

    public ApiException(String code, String message, HttpStatus status, List<FieldIssue> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = details;
    }
}
