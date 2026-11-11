package com.bankbatch.servicio;

import com.bankbatch.dto.RespuestaCuenta;
import com.bankbatch.dto.RespuestaPagina;
import com.bankbatch.dto.SolicitudAltaCuenta;
import com.bankbatch.entidad.AccionAuditoria;
import com.bankbatch.entidad.Cliente;
import com.bankbatch.entidad.Cuenta;
import com.bankbatch.entidad.EntidadAuditada;
import com.bankbatch.entidad.EstadoCuenta;
import com.bankbatch.excepcion.ConflictoNegocioExcepcion;
import com.bankbatch.excepcion.RecursoNoEncontradoExcepcion;
import com.bankbatch.repositorio.RepositorioClientes;
import com.bankbatch.repositorio.RepositorioCuentas;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Service
public class ServicioCuentas {

    private static final int TAMANO_MAXIMO_DE_PAGINA = 100;

    private final RepositorioCuentas repositorioCuentas;
    private final RepositorioClientes repositorioClientes;
    private final ServicioAuditoria servicioAuditoria;

    public ServicioCuentas(RepositorioCuentas repositorioCuentas, RepositorioClientes repositorioClientes,
                           ServicioAuditoria servicioAuditoria) {
        this.repositorioCuentas = repositorioCuentas;
        this.repositorioClientes = repositorioClientes;
        this.servicioAuditoria = servicioAuditoria;
    }

    @Transactional
    public RespuestaCuenta crear(SolicitudAltaCuenta solicitud) {
        Cliente cliente = repositorioClientes.findById(solicitud.idCliente())
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion(
                        "No existe el cliente con id " + solicitud.idCliente()));
        if (!cliente.estaActivo()) {
            throw new ConflictoNegocioExcepcion("No se puede abrir una cuenta para un cliente dado de baja");
        }

        String numero = String.format(Locale.ROOT, "%s-%010d",
                solicitud.tipoCuenta().getPrefijo(), repositorioCuentas.siguienteNumeroDeCuenta());
        Cuenta cuenta = repositorioCuentas.saveAndFlush(
                new Cuenta(cliente.getId(), solicitud.tipoCuenta(), numero));

        RespuestaCuenta respuesta = RespuestaCuenta.desde(cuenta);
        servicioAuditoria.registrar(EntidadAuditada.CUENTA, cuenta.getId(), AccionAuditoria.CREACION, null, respuesta);
        return respuesta;
    }

    @Transactional(readOnly = true)
    public RespuestaPagina<RespuestaCuenta> buscar(Long idCliente, EstadoCuenta estado, int pagina, int tamano) {
        Collection<EstadoCuenta> estados = estado == null ? List.of(EstadoCuenta.values()) : List.of(estado);
        Pageable paginacion = PageRequest.of(
                Math.max(pagina, 0),
                Math.min(Math.max(tamano, 1), TAMANO_MAXIMO_DE_PAGINA),
                Sort.by("numeroCuenta"));

        Page<Cuenta> resultado = idCliente == null
                ? repositorioCuentas.findByEstadoIn(estados, paginacion)
                : repositorioCuentas.findByIdClienteAndEstadoIn(idCliente, estados, paginacion);
        return RespuestaPagina.desde(resultado.map(RespuestaCuenta::desde));
    }

    @Transactional(readOnly = true)
    public RespuestaCuenta obtener(Long id) {
        return RespuestaCuenta.desde(obtenerEntidad(id));
    }

    @Transactional
    public RespuestaCuenta bloquear(Long id) {
        Cuenta cuenta = obtenerEntidad(id);
        if (cuenta.getEstado() != EstadoCuenta.ACTIVA) {
            throw new ConflictoNegocioExcepcion("Solo una cuenta ACTIVA puede bloquearse");
        }
        return cambiarEstado(cuenta, AccionAuditoria.MODIFICACION, cuenta::bloquear);
    }

    @Transactional
    public RespuestaCuenta desbloquear(Long id) {
        Cuenta cuenta = obtenerEntidad(id);
        if (cuenta.getEstado() != EstadoCuenta.BLOQUEADA) {
            throw new ConflictoNegocioExcepcion("Solo una cuenta BLOQUEADA puede desbloquearse");
        }
        return cambiarEstado(cuenta, AccionAuditoria.MODIFICACION, cuenta::desbloquear);
    }

    @Transactional
    public RespuestaCuenta cerrar(Long id) {
        Cuenta cuenta = obtenerEntidad(id);
        if (cuenta.getEstado() == EstadoCuenta.CERRADA) {
            throw new ConflictoNegocioExcepcion("La cuenta ya está cerrada");
        }
        if (cuenta.getSaldo().signum() != 0) {
            throw new ConflictoNegocioExcepcion("No se puede cerrar una cuenta con saldo distinto de cero");
        }
        return cambiarEstado(cuenta, AccionAuditoria.BAJA_LOGICA, cuenta::cerrar);
    }

    private RespuestaCuenta cambiarEstado(Cuenta cuenta, AccionAuditoria accion, Runnable cambio) {
        RespuestaCuenta antes = RespuestaCuenta.desde(cuenta);
        cambio.run();
        repositorioCuentas.saveAndFlush(cuenta);

        RespuestaCuenta despues = RespuestaCuenta.desde(cuenta);
        servicioAuditoria.registrar(EntidadAuditada.CUENTA, cuenta.getId(), accion, antes, despues);
        return despues;
    }

    private Cuenta obtenerEntidad(Long id) {
        return repositorioCuentas.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("No existe la cuenta con id " + id));
    }
}
