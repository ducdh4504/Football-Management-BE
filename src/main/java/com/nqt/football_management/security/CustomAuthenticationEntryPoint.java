package com.nqt.football_management.security;



import com.nqt.football_management.constants.ErrorCode;
import com.nqt.football_management.model.response.APIResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        log.warn(
                "Authentication failed | method={} | path={} | message={}",
                request.getMethod(),
                request.getRequestURI(),
                authException.getMessage()
        );

        ErrorCode errorCode =
                ErrorCode.UNAUTHENTICATED;

        response.setStatus(
                errorCode.getStatus().value()
        );

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        response.setCharacterEncoding(
                "UTF-8"
        );

        APIResponse<Object> body =
                APIResponse.response(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        null,
                        false
                );

        jsonMapper.writeValue(
                response.getOutputStream(),
                body
        );
    }
}
