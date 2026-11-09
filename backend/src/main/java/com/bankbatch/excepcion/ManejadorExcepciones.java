package com.bankbatch.excepcion;

import com.bankbatch.dto.RespuestaError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ManejadorExcepciones {

    @ExceptionHandler(CredencialesInvalidasExcepcion.class)
    public ResponseEntity<RespuestaError> credencialesInvalidas(CredencialesInvalidasExcepcion excepcion,
                                                                HttpServletRequest solicitud) {
        return responder(HttpStatus.UNAUTHORIZED, "No autenticado", excepcion.getMessage(), solicitud, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> datosInvalidos(MethodArgumentNotValidException excepcion,
                                                         HttpServletRequest solicitud) {
        List<String> detalles = excepcion.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .sorted()
            .toList();
        return responder(HttpStatus.BAD_REQUEST, "Solicitud incorrecta",
            "La solicitud contiene datos inválidos", solicitud, detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaError> cuerpoIlegible(HttpMessageNotReadableException excepcion,
                                                         HttpServletRequest solicitud) {
        return responder(HttpStatus.BAD_REQUEST, "Solicitud incorrecta",
            "El cuerpo de la solicitud falta o no tiene un formato válido", solicitud, List.of());
    }

    private ResponseEntity<RespuestaError> responder(HttpStatus estado, String error, String mensaje,
                                                     HttpServletRequest solicitud, List<String> detalles) {
        RespuestaError cuerpo = new RespuestaError(
            Instant.now(), estado.value(), error, mensaje, solicitud.getRequestURI(), detalles);
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
