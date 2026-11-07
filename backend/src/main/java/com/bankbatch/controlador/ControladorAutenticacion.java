package com.bankbatch.controlador;

import com.bankbatch.dto.RespuestaAutenticacion;
import com.bankbatch.dto.RespuestaPerfil;
import com.bankbatch.dto.SolicitudInicioSesion;
import com.bankbatch.servicio.ServicioAutenticacion;
import com.bankbatch.servicio.ServicioUsuarios;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/autenticacion")
public class ControladorAutenticacion {

    private final ServicioAutenticacion servicioAutenticacion;
    private final ServicioUsuarios servicioUsuarios;

    public ControladorAutenticacion(ServicioAutenticacion servicioAutenticacion, ServicioUsuarios servicioUsuarios) {
        this.servicioAutenticacion = servicioAutenticacion;
        this.servicioUsuarios = servicioUsuarios;
    }

    @PostMapping("/inicio-sesion")
    public RespuestaAutenticacion iniciarSesion(@Valid @RequestBody SolicitudInicioSesion solicitud) {
        return servicioAutenticacion.iniciarSesion(solicitud);
    }

    @GetMapping("/perfil")
    public RespuestaPerfil perfil(Authentication autenticacion) {
        return servicioUsuarios.obtenerPerfil(autenticacion.getName());
    }
}
