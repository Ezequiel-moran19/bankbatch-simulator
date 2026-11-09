package com.bankbatch.repositorio;

import com.bankbatch.entidad.AccionAuditoria;
import com.bankbatch.entidad.EntidadAuditada;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class RepositorioAuditoria {

    public record Registro(Long id, Long idUsuario, String entidad, Long idRegistro, String accion,
                           String datosAnteriores, String datosNuevos, Instant fecha) {
    }

    private final JdbcClient jdbc;

    public RepositorioAuditoria(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insertar(Long idUsuario, EntidadAuditada entidad, Long idRegistro, AccionAuditoria accion,
                         String datosAnteriores, String datosNuevos) {
        jdbc.sql("""
                INSERT INTO auditoria (id_usuario, entidad, id_registro, accion, datos_anteriores, datos_nuevos)
                VALUES (:idUsuario, :entidad, :idRegistro, :accion,
                        CAST(:datosAnteriores AS jsonb), CAST(:datosNuevos AS jsonb))
                """)
            .param("idUsuario", idUsuario)
            .param("entidad", entidad.name())
            .param("idRegistro", idRegistro)
            .param("accion", accion.name())
            .param("datosAnteriores", datosAnteriores)
            .param("datosNuevos", datosNuevos)
            .update();
    }

    public List<Registro> consultar(EntidadAuditada entidad, Long idRegistro, int limite) {
        return jdbc.sql("""
                SELECT id_auditoria, id_usuario, entidad, id_registro, accion,
                       CAST(datos_anteriores AS text) AS datos_anteriores,
                       CAST(datos_nuevos AS text) AS datos_nuevos,
                       fecha_evento
                FROM auditoria
                WHERE (CAST(:entidad AS varchar) IS NULL OR entidad = CAST(:entidad AS varchar))
                  AND (CAST(:idRegistro AS bigint) IS NULL OR id_registro = CAST(:idRegistro AS bigint))
                ORDER BY fecha_evento DESC, id_auditoria DESC
                LIMIT :limite
                """)
            .param("entidad", entidad == null ? null : entidad.name())
            .param("idRegistro", idRegistro)
            .param("limite", limite)
            .query((filaResultado, numeroFila) -> new Registro(
                filaResultado.getLong("id_auditoria"),
                filaResultado.getObject("id_usuario", Long.class),
                filaResultado.getString("entidad"),
                filaResultado.getLong("id_registro"),
                filaResultado.getString("accion"),
                filaResultado.getString("datos_anteriores"),
                filaResultado.getString("datos_nuevos"),
                filaResultado.getObject("fecha_evento", OffsetDateTime.class).toInstant()))
            .list();
    }
}
