package com.univerliga.crm.util;

import com.univerliga.crm.dto.ApiMeta;
import com.univerliga.crm.dto.ApiResponse;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApiResponseFactory {

    private final String version;

    public ApiResponseFactory(@Value("${app.api-version:v1}") String version) {
        this.version = version;
    }

    public <T> ApiResponse<T> success(T data) {
        ApiMeta meta = new ApiMeta(RequestIdHolder.getOrGenerate(), OffsetDateTime.now().toString(), version);
        return new ApiResponse<>(data, meta);
    }
}
