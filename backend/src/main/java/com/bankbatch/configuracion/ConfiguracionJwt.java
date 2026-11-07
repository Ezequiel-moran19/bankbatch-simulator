package com.bankbatch.configuracion;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(PropiedadesJwt.class)
public class ConfiguracionJwt {

    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }

    @Bean
    public JwtEncoder codificadorJwt(PropiedadesJwt propiedades) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveSecreta(propiedades)));
    }

    @Bean
    public JwtDecoder decodificadorJwt(PropiedadesJwt propiedades) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder
                .withSecretKey(claveSecreta(propiedades))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decodificador.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(Duration.ZERO)));
        return decodificador;
    }

    private static SecretKey claveSecreta(PropiedadesJwt propiedades) {
        return new SecretKeySpec(propiedades.secreto().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
