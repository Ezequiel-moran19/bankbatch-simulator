package com.bankbatch.controlador;

import com.bankbatch.dto.RespuestaPagina;
import com.bankbatch.dto.RespuestaUsuario;
import com.bankbatch.dto.SolicitudAltaUsuario;
import com.bankbatch.dto.SolicitudCambioEstadoUsuario;
import com.bankbatch.dto.SolicitudModificacionUsuario;
import com.bankbatch.entidad.EstadoUsuario;
import com.bankbatch.servicio.ServicioUsuarios;
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
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ControladorUsuarios {

    private final ServicioUsuarios servicioUsuarios;

    public ControladorUsuarios(ServicioUsuarios servicioUsuarios) {
        this.servicioUsuarios = servicioUsuarios;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaUsuario crear(@Valid @RequestBody SolicitudAltaUsuario solicitud) {
        return servicioUsuarios.crear(solicitud);
    }

    @GetMapping
    public RespuestaPagina<RespuestaUsuario> buscar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) EstadoUsuario estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return servicioUsuarios.buscar(texto, estado, pagina, tamano);
    }

    @GetMapping("/{id}")
    public RespuestaUsuario obtener(@PathVariable Long id) {
        return servicioUsuarios.obtener(id);
    }

    @PutMapping("/{id}")
    public RespuestaUsuario modificar(@PathVariable Long id,
                                      @Valid @RequestBody SolicitudModificacionUsuario solicitud) {
        return servicioUsuarios.modificar(id, solicitud);
    }

    @PutMapping("/{id}/estado")
    public RespuestaUsuario cambiarEstado(@PathVariable Long id,
                                          @Valid @RequestBody SolicitudCambioEstadoUsuario solicitud) {
        return servicioUsuarios.cambiarEstado(id, solicitud.estado());
    }
}
