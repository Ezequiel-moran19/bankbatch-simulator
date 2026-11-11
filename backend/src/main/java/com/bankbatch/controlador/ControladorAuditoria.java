package com.bankbatch.controlador;

import com.bankbatch.dto.RespuestaAuditoria;
import com.bankbatch.entidad.EntidadAuditada;
import com.bankbatch.servicio.ServicioAuditoria;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
public class ControladorAuditoria {

    private final ServicioAuditoria servicioAuditoria;

    public ControladorAuditoria(ServicioAuditoria servicioAuditoria) {
        this.servicioAuditoria = servicioAuditoria;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<RespuestaAuditoria> consultar(
        @RequestParam(required = false) EntidadAuditada entidad,
        @RequestParam(required = false) Long idRegistro) {
        return servicioAuditoria.consultar(entidad, idRegistro);
    }
}
