package com.bankbatch.servicio;

import com.bankbatch.dto.RespuestaCliente;
import com.bankbatch.dto.RespuestaPagina;
import com.bankbatch.dto.SolicitudAltaCliente;
import com.bankbatch.dto.SolicitudModificacionCliente;
import com.bankbatch.entidad.AccionAuditoria;
import com.bankbatch.entidad.Cliente;
import com.bankbatch.entidad.EntidadAuditada;
import com.bankbatch.entidad.EstadoCliente;
import com.bankbatch.excepcion.ConflictoNegocioExcepcion;
import com.bankbatch.excepcion.RecursoNoEncontradoExcepcion;
import com.bankbatch.repositorio.RepositorioClientes;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Service
public class ServicioClientes {

    private static final int TAMANO_MAXIMO_DE_PAGINA = 100;

    private final RepositorioClientes repositorioClientes;
    private final ServicioAuditoria servicioAuditoria;

    public ServicioClientes(RepositorioClientes repositorioClientes, ServicioAuditoria servicioAuditoria) {
        this.repositorioClientes = repositorioClientes;
        this.servicioAuditoria = servicioAuditoria;
    }

    @Transactional
    public RespuestaCliente crear(SolicitudAltaCliente solicitud) {
        String documento = solicitud.documento().strip();
        if (repositorioClientes.existsByDocumento(documento)) {
            throw new ConflictoNegocioExcepcion("Ya existe un cliente con el documento " + documento);
        }

        Cliente cliente = repositorioClientes.saveAndFlush(new Cliente(
            solicitud.nombre(), solicitud.apellido(), documento, solicitud.correo(), solicitud.telefono()));

        RespuestaCliente respuesta = RespuestaCliente.desde(cliente);
        servicioAuditoria.registrar(EntidadAuditada.CLIENTE, cliente.getId(), AccionAuditoria.CREACION, null, respuesta);
        return respuesta;
    }

    @Transactional(readOnly = true)
    public RespuestaPagina<RespuestaCliente> buscar(String texto, EstadoCliente estado, int pagina, int tamano) {
        String patron = (texto == null || texto.isBlank()) ? "%" : "%" + texto.strip().toLowerCase(Locale.ROOT) + "%";
        Collection<EstadoCliente> estados = estado == null ? List.of(EstadoCliente.values()) : List.of(estado);
        Pageable paginacion = PageRequest.of(
            Math.max(pagina, 0),
            Math.min(Math.max(tamano, 1), TAMANO_MAXIMO_DE_PAGINA),
            Sort.by("apellido", "nombre"));

        return RespuestaPagina.desde(repositorioClientes.buscar(patron, estados, paginacion)
            .map(RespuestaCliente::desde));
    }

    @Transactional(readOnly = true)
    public RespuestaCliente obtener(Long id) {
        return RespuestaCliente.desde(obtenerEntidad(id));
    }

    @Transactional
    public RespuestaCliente modificar(Long id, SolicitudModificacionCliente solicitud) {
        Cliente cliente = obtenerEntidad(id);
        if (!cliente.estaActivo()) {
            throw new ConflictoNegocioExcepcion("El cliente está dado de baja y no puede modificarse");
        }

        RespuestaCliente antes = RespuestaCliente.desde(cliente);
        cliente.actualizarDatos(solicitud.nombre(), solicitud.apellido(), solicitud.correo(), solicitud.telefono());
        repositorioClientes.saveAndFlush(cliente);

        RespuestaCliente despues = RespuestaCliente.desde(cliente);
        servicioAuditoria.registrar(EntidadAuditada.CLIENTE, id, AccionAuditoria.MODIFICACION, antes, despues);
        return despues;
    }

    @Transactional
    public RespuestaCliente darDeBaja(Long id) {
        Cliente cliente = obtenerEntidad(id);
        if (!cliente.estaActivo()) {
            throw new ConflictoNegocioExcepcion("El cliente ya está dado de baja");
        }
        if (repositorioClientes.contarCuentasNoCerradas(id) > 0) {
            throw new ConflictoNegocioExcepcion(
                "No se puede dar de baja al cliente: tiene cuentas que no están cerradas");
        }

        RespuestaCliente antes = RespuestaCliente.desde(cliente);
        cliente.darDeBaja();
        repositorioClientes.saveAndFlush(cliente);

        RespuestaCliente despues = RespuestaCliente.desde(cliente);
        servicioAuditoria.registrar(EntidadAuditada.CLIENTE, id, AccionAuditoria.BAJA_LOGICA, antes, despues);
        return despues;
    }

    private Cliente obtenerEntidad(Long id) {
        return repositorioClientes.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoExcepcion("No existe el cliente con id " + id));
    }
}
