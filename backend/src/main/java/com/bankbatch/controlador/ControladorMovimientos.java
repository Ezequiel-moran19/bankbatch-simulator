package com.bankbatch.controlador;

import com.bankbatch.dto.RespuestaMovimiento;
import com.bankbatch.dto.RespuestaPagina;
import com.bankbatch.entidad.TipoMovimiento;
import com.bankbatch.servicio.ServicioMovimientos;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/movimientos")
public class ControladorMovimientos {

    private final ServicioMovimientos servicioMovimientos;

    public ControladorMovimientos(ServicioMovimientos servicioMovimientos) {
        this.servicioMovimientos = servicioMovimientos;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public RespuestaPagina<RespuestaMovimiento> buscar(
        @RequestParam(required = false) Long idCuenta,
        @RequestParam(required = false) Long idEjecucion,
        @RequestParam(required = false) TipoMovimiento tipo,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        @RequestParam(defaultValue = "0") int pagina,
        @RequestParam(defaultValue = "20") int tamano) {
        return servicioMovimientos.buscar(idCuenta, idEjecucion, tipo, desde, hasta, pagina, tamano);
    }
}
