package com.bankbatch.configuracion;

import com.bankbatch.dto.RespuestaError;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
public class RespuestasSeguridadJson implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RespuestasSeguridadJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest solicitud, HttpServletResponse respuesta,
        AuthenticationException excepcion) throws IOException {

        respuesta.setHeader("WWW-Authenticate", "Bearer");

        responder(solicitud, respuesta, HttpStatus.UNAUTHORIZED,
            "No autenticado", "Se requiere autenticación para acceder al recurso."
        );
    }

    @Override
    public void handle(
        HttpServletRequest solicitud,
        HttpServletResponse respuesta,
        AccessDeniedException excepcion) throws IOException {

        responder(solicitud, respuesta, HttpStatus.FORBIDDEN,
            "Acceso denegado", "No tenés permisos para acceder al recurso."
        );
    }

    private void responder(HttpServletRequest solicitud, HttpServletResponse respuesta, HttpStatus estado,
        String error, String mensaje) throws IOException {

        RespuestaError cuerpo = new RespuestaError(Instant.now(), estado.value(), error,
            mensaje, solicitud.getRequestURI(), List.of()
        );

        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(respuesta.getWriter(), cuerpo);
    }
}
