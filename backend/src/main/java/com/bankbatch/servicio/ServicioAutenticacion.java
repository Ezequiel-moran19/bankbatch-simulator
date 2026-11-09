package com.bankbatch.servicio;

import com.bankbatch.dto.RespuestaAutenticacion;
import com.bankbatch.dto.SolicitudInicioSesion;
import com.bankbatch.entidad.Rol;
import com.bankbatch.entidad.Usuario;
import com.bankbatch.excepcion.CredencialesInvalidasExcepcion;
import com.bankbatch.repositorio.RepositorioUsuarios;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ServicioAutenticacion {

    private final RepositorioUsuarios repositorioUsuarios;
    private final PasswordEncoder codificador;
    private final ServicioJwt servicioJwt;
    private final Clock reloj;
    private final String hashFicticio;

    public ServicioAutenticacion(RepositorioUsuarios repositorioUsuarios, PasswordEncoder codificador,
                                 ServicioJwt servicioJwt, Clock reloj) {
        this.repositorioUsuarios = repositorioUsuarios;
        this.codificador = codificador;
        this.servicioJwt = servicioJwt;
        this.reloj = reloj;
        this.hashFicticio = codificador.encode("contrasena-ficticia");
    }

    @Transactional
    public RespuestaAutenticacion iniciarSesion(SolicitudInicioSesion solicitud) {
        if (ValidadorContrasena.excedeElLimite(solicitud.contrasena())) {
            throw new CredencialesInvalidasExcepcion();
        }
        String nombreUsuario = solicitud.nombreUsuario().strip().toLowerCase(Locale.ROOT);
        Optional<Usuario> encontrado = repositorioUsuarios.findByNombreUsuario(nombreUsuario);

        String hash = encontrado.map(Usuario::getContrasenaHash).orElse(hashFicticio);
        boolean contrasenaCorrecta = codificador.matches(solicitud.contrasena(), hash);

        if (encontrado.isEmpty() || !contrasenaCorrecta || !encontrado.get().estaActivo()) {
            throw new CredencialesInvalidasExcepcion();
        }

        Usuario usuario = encontrado.get();
        usuario.registrarAcceso(reloj.instant());
        repositorioUsuarios.save(usuario);

        ServicioJwt.TokenEmitido token = servicioJwt.emitir(usuario);
        List<String> roles = usuario.getRoles().stream().map(Rol::getNombre).sorted().toList();
        return new RespuestaAutenticacion(token.valor(), "Bearer", token.expiraEn(), roles);
    }
}
