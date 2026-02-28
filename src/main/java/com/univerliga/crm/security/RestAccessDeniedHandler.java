package com.univerliga.crm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.univerliga.crm.dto.ErrorBody;
import com.univerliga.crm.dto.ErrorResponse;
import com.univerliga.crm.util.RequestIdHolder;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorBody body = new ErrorBody("FORBIDDEN", "Access denied", List.of(), RequestIdHolder.getOrGenerate());
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(body));
    }
}
