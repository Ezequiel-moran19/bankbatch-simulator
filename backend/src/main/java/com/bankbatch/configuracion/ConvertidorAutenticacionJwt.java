package com.bankbatch.configuracion;

import com.bankbatch.entidad.Usuario;
import com.bankbatch.repositorio.RepositorioUsuarios;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConvertidorAutenticacionJwt implements Converter<Jwt, AbstractAuthenticationToken> {

    private final RepositorioUsuarios repositorioUsuarios;

    public ConvertidorAutenticacionJwt(RepositorioUsuarios repositorioUsuarios) {
        this.repositorioUsuarios = repositorioUsuarios;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String nombreUsuario = jwt.getSubject();
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            throw new InvalidBearerTokenException("El token no identifica a un usuario");
        }

        Usuario usuario = repositorioUsuarios.findByNombreUsuario(nombreUsuario)
                .filter(Usuario::estaActivo)
                .orElseThrow(() -> new InvalidBearerTokenException("Usuario inexistente o no habilitado"));

        List<GrantedAuthority> autoridades = usuario.getRoles().stream()
                .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + rol.getNombre()))
                .toList();

        return new JwtAuthenticationToken(jwt, autoridades, usuario.getNombreUsuario());
    }
}
