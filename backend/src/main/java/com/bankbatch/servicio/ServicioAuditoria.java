package com.bankbatch.servicio;

import com.bankbatch.dto.RespuestaAuditoria;
import com.bankbatch.entidad.AccionAuditoria;
import com.bankbatch.entidad.EntidadAuditada;
import com.bankbatch.repositorio.RepositorioAuditoria;
import com.bankbatch.repositorio.RepositorioUsuarios;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class ServicioAuditoria {

    private static final int LIMITE_DE_CONSULTA = 200;

    private final RepositorioAuditoria repositorioAuditoria;
    private final RepositorioUsuarios repositorioUsuarios;
    private final ObjectMapper mapeador;

    public ServicioAuditoria(RepositorioAuditoria repositorioAuditoria, RepositorioUsuarios repositorioUsuarios,
                             ObjectMapper mapeador) {
        this.repositorioAuditoria = repositorioAuditoria;
        this.repositorioUsuarios = repositorioUsuarios;
        this.mapeador = mapeador;
    }

    @Transactional
    public void registrar(EntidadAuditada entidad, Long idRegistro, AccionAuditoria accion,
                          Object datosAnteriores, Object datosNuevos) {
        repositorioAuditoria.insertar(
            idUsuarioActual(), entidad, idRegistro, accion, aJson(datosAnteriores), aJson(datosNuevos));
    }

    @Transactional(readOnly = true)
    public List<RespuestaAuditoria> consultar(EntidadAuditada entidad, Long idRegistro) {
        return repositorioAuditoria.consultar(entidad, idRegistro, LIMITE_DE_CONSULTA).stream()
            .map(registro -> new RespuestaAuditoria(
                registro.id(),
                registro.idUsuario(),
                registro.entidad(),
                registro.idRegistro(),
                registro.accion(),
                leer(registro.datosAnteriores()),
                leer(registro.datosNuevos()),
                registro.fecha()))
            .toList();
    }

    private Long idUsuarioActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()
            || autenticacion instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return repositorioUsuarios.buscarIdPorNombreUsuario(autenticacion.getName()).orElse(null);
    }

    private String aJson(Object datos) {
        return datos == null ? null : mapeador.writeValueAsString(datos);
    }

    private JsonNode leer(String json) {
        return json == null ? null : mapeador.readTree(json);
    }
}
