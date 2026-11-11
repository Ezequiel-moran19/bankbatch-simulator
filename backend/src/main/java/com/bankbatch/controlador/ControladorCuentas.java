package com.bankbatch.controlador;

import com.bankbatch.dto.RespuestaCuenta;
import com.bankbatch.dto.RespuestaPagina;
import com.bankbatch.dto.SolicitudAltaCuenta;
import com.bankbatch.entidad.EstadoCuenta;
import com.bankbatch.servicio.ServicioCuentas;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cuentas")
public class ControladorCuentas {

    private final ServicioCuentas servicioCuentas;

    public ControladorCuentas(ServicioCuentas servicioCuentas) {
        this.servicioCuentas = servicioCuentas;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public RespuestaCuenta crear(@Valid @RequestBody SolicitudAltaCuenta solicitud) {
        return servicioCuentas.crear(solicitud);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public RespuestaPagina<RespuestaCuenta> buscar(
            @RequestParam(required = false) Long idCliente,
            @RequestParam(required = false) EstadoCuenta estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return servicioCuentas.buscar(idCliente, estado, pagina, tamano);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public RespuestaCuenta obtener(@PathVariable Long id) {
        return servicioCuentas.obtener(id);
    }

    @PostMapping("/{id}/bloqueo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public RespuestaCuenta bloquear(@PathVariable Long id) {
        return servicioCuentas.bloquear(id);
    }

    @PostMapping("/{id}/desbloqueo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public RespuestaCuenta desbloquear(@PathVariable Long id) {
        return servicioCuentas.desbloquear(id);
    }

    @PostMapping("/{id}/baja")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public RespuestaCuenta cerrar(@PathVariable Long id) {
        return servicioCuentas.cerrar(id);
    }
}
