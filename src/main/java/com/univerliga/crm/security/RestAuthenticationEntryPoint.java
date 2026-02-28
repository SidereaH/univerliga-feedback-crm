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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorBody body = new ErrorBody("UNAUTHORIZED", "Authentication is required", List.of(), RequestIdHolder.getOrGenerate());
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(body));
    }
}
