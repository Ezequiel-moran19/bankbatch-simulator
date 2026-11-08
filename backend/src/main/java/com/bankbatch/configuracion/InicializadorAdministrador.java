package com.bankbatch.configuracion;

import com.bankbatch.entidad.Rol;
import com.bankbatch.entidad.Usuario;
import com.bankbatch.repositorio.RepositorioRoles;
import com.bankbatch.repositorio.RepositorioUsuarios;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@Profile("dev")
public class InicializadorAdministrador implements ApplicationRunner, EnvironmentAware {

    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioRoles repositorioRoles;
    private final PasswordEncoder codificadorContrasenas;
    private Environment entorno;

    public InicializadorAdministrador(RepositorioUsuarios repositorioUsuarios, RepositorioRoles repositorioRoles, PasswordEncoder codificadorContrasenas) {
        this.repositorioUsuarios = repositorioUsuarios;
        this.repositorioRoles = repositorioRoles;
        this.codificadorContrasenas = codificadorContrasenas;
    }

    @Override
    public void setEnvironment(Environment entorno) {
        this.entorno = entorno;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (repositorioUsuarios.count() > 0) {
            return;
        }

        String contrasena = entorno.getProperty("BANKBATCH_ADMIN_CLAVE_INICIAL");

        if (!StringUtils.hasText(contrasena)) {
            throw new IllegalStateException("No se puede crear el administrador: falta BANKBATCH_ADMIN_CLAVE_INICIAL.");
        }

        String nombreUsuario = entorno.getProperty(
                "BANKBATCH_ADMIN_NOMBRE_USUARIO", "admin");
        String correo = entorno.getProperty(
                "BANKBATCH_ADMIN_CORREO", "admin@bankbatch.local");

        if (!StringUtils.hasText(nombreUsuario) || !StringUtils.hasText(correo)) {
            throw new IllegalStateException("El nombre de usuario y el correo del administrador son obligatorios.");
        }

        Rol rolAdministrador = repositorioRoles.findByNombre("ADMINISTRADOR")
                .orElseThrow(() -> new IllegalStateException("No existe el rol ADMINISTRADOR en la base de datos."));

        Usuario administrador = new Usuario(
                "Administrador",
                "Sistema",
                correo.strip(),
                nombreUsuario.strip(),
                codificadorContrasenas.encode(contrasena));

        administrador.agregarRol(rolAdministrador);
        repositorioUsuarios.save(administrador);
    }
}
