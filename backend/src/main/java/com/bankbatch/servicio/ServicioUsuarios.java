package com.bankbatch.servicio;

import com.bankbatch.dto.RespuestaPerfil;
import com.bankbatch.entidad.Rol;
import com.bankbatch.entidad.Usuario;
import com.bankbatch.excepcion.CredencialesInvalidasExcepcion;
import com.bankbatch.repositorio.RepositorioUsuarios;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServicioUsuarios {

    private final RepositorioUsuarios repositorioUsuarios;

    public ServicioUsuarios(RepositorioUsuarios repositorioUsuarios) {
        this.repositorioUsuarios = repositorioUsuarios;
    }

    @Transactional(readOnly = true)
    public RespuestaPerfil obtenerPerfil(String nombreUsuario) {
        Usuario usuario = repositorioUsuarios.findByNombreUsuario(nombreUsuario)
                .orElseThrow(CredencialesInvalidasExcepcion::new);
        List<String> roles = usuario.getRoles().stream().map(Rol::getNombre).sorted().toList();
        return new RespuestaPerfil(
                usuario.getNombreUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                roles,
                usuario.getUltimoAcceso());
    }
}
