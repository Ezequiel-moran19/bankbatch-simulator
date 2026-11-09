package com.bankbatch.apoyo;

import com.bankbatch.entidad.Usuario;
import com.bankbatch.repositorio.RepositorioRoles;
import com.bankbatch.repositorio.RepositorioUsuarios;
import com.bankbatch.servicio.ServicioJwt;

public class AyudanteAutenticacion {

    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioRoles repositorioRoles;
    private final ServicioJwt servicioJwt;

    public AyudanteAutenticacion(RepositorioUsuarios repositorioUsuarios, RepositorioRoles repositorioRoles,
                                 ServicioJwt servicioJwt) {
        this.repositorioUsuarios = repositorioUsuarios;
        this.repositorioRoles = repositorioRoles;
        this.servicioJwt = servicioJwt;
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }

    public String tokenAdministrador() {
        return tokenPara("administrador_prueba", "ADMINISTRADOR");
    }

    public String tokenOperador() {
        return tokenPara("operador_prueba", "OPERADOR");
    }

    private String tokenPara(String nombreUsuario, String nombreRol) {
        Usuario usuario = new Usuario("Prueba", nombreRol, nombreUsuario + "@correo.test", nombreUsuario, "hash");
        usuario.agregarRol(repositorioRoles.findByNombre(nombreRol).orElseThrow());
        repositorioUsuarios.saveAndFlush(usuario);
        return servicioJwt.emitir(usuario).valor();
    }
}
