package com.bankbatch.dto;

import com.bankbatch.entidad.EstadoUsuario;
import com.bankbatch.entidad.Rol;
import com.bankbatch.entidad.Usuario;

import java.time.Instant;
import java.util.List;

public record RespuestaUsuario(
        Long id,
        String nombre,
        String apellido,
        String correo,
        String nombreUsuario,
        EstadoUsuario estado,
        List<String> roles,
        Instant fechaCreacion,
        Instant ultimoAcceso) {

    public static RespuestaUsuario desde(Usuario usuario) {
        return new RespuestaUsuario(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                usuario.getNombreUsuario(),
                usuario.getEstado(),
                usuario.getRoles().stream().map(Rol::getNombre).sorted().toList(),
                usuario.getFechaCreacion(),
                usuario.getUltimoAcceso());
    }
}
