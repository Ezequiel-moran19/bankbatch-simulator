package com.bankbatch.configuracion;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PropiedadesJwtTests {

    private final ApplicationContextRunner ejecutor =
            new ApplicationContextRunner().withUserConfiguration(ConfiguracionJwt.class);

    @Test
    void aceptaUnSecretoValidoYUsaQuinceMinutosPorDefecto() {
        ejecutor
                .withPropertyValues("bankbatch.seguridad.jwt.secreto=0123456789abcdef0123456789abcdef")
                .run(contexto -> {
                    assertThat(contexto).hasNotFailed();
                    PropiedadesJwt propiedades = contexto.getBean(PropiedadesJwt.class);
                    assertThat(propiedades.duracion()).isEqualTo(Duration.ofMinutes(15));
                });
    }

    @Test
    void rechazaUnSecretoCorto() {
        ejecutor
                .withPropertyValues("bankbatch.seguridad.jwt.secreto=corto")
                .run(contexto -> assertThat(contexto).hasFailed());
    }

    @Test
    void rechazaLaFaltaDeSecreto() {
        ejecutor.run(contexto -> assertThat(contexto).hasFailed());
    }
}
