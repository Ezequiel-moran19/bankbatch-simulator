package com.bankbatch.servicio;

import com.bankbatch.configuracion.PropiedadesJwt;
import com.bankbatch.entidad.Rol;
import com.bankbatch.entidad.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class ServicioJwt {

    public record TokenEmitido(String valor, Instant expiraEn) {
    }

    private final JwtEncoder codificador;
    private final JwtDecoder decodificador;
    private final PropiedadesJwt propiedades;
    private final Clock reloj;

    public ServicioJwt(JwtEncoder codificador, JwtDecoder decodificador, PropiedadesJwt propiedades, Clock reloj) {
        this.codificador = codificador;
        this.decodificador = decodificador;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    public TokenEmitido emitir(Usuario usuario) {
        Instant ahora = reloj.instant();
        Instant expiracion = ahora.plus(propiedades.duracion());
        List<String> roles = usuario.getRoles().stream().map(Rol::getNombre).sorted().toList();

        JwtClaimsSet reclamos = JwtClaimsSet.builder()
                .subject(usuario.getNombreUsuario())
                .issuedAt(ahora)
                .expiresAt(expiracion)
                .claim("roles", roles)
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();

        String valor = codificador.encode(JwtEncoderParameters.from(cabecera, reclamos)).getTokenValue();
        return new TokenEmitido(valor, expiracion);
    }

    public Jwt validar(String token) {
        return decodificador.decode(token);
    }
}
