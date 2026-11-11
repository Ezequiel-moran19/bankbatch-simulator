package com.bankbatch.controlador;

import com.bankbatch.dto.RespuestaCliente;
import com.bankbatch.dto.RespuestaPagina;
import com.bankbatch.dto.SolicitudAltaCliente;
import com.bankbatch.dto.SolicitudModificacionCliente;
import com.bankbatch.entidad.EstadoCliente;
import com.bankbatch.servicio.ServicioClientes;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
public class ControladorClientes {

    private final ServicioClientes servicioClientes;

    public ControladorClientes(ServicioClientes servicioClientes) {
        this.servicioClientes = servicioClientes;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public RespuestaCliente crear(@Valid @RequestBody SolicitudAltaCliente solicitud) {
        return servicioClientes.crear(solicitud);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public RespuestaPagina<RespuestaCliente> buscar(
        @RequestParam(required = false) String texto,
        @RequestParam(required = false) EstadoCliente estado,
        @RequestParam(defaultValue = "0") int pagina,
        @RequestParam(defaultValue = "20") int tamano) {
        return servicioClientes.buscar(texto, estado, pagina, tamano);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public RespuestaCliente obtener(@PathVariable Long id) {
        return servicioClientes.obtener(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public RespuestaCliente modificar(@PathVariable Long id,
                                      @Valid @RequestBody SolicitudModificacionCliente solicitud) {
        return servicioClientes.modificar(id, solicitud);
    }

    @PostMapping("/{id}/baja")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public RespuestaCliente darDeBaja(@PathVariable Long id) {
        return servicioClientes.darDeBaja(id);
    }
}
