package com.bankbatch.configuracion;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PropiedadesJwt.class)
public class ConfiguracionJwt {
}
