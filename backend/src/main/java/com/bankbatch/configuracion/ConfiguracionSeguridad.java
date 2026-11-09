package com.bankbatch.configuracion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ConfiguracionSeguridad {

    @Bean
    public SecurityFilterChain cadenaDeSeguridad(
        HttpSecurity http,
        JwtDecoder decodificador,
        ConvertidorAutenticacionJwt convertidor,
        RespuestasSeguridadJson respuestasSeguridadJson) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(sesion ->
                sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(excepciones -> excepciones
                .authenticationEntryPoint(respuestasSeguridadJson)
                .accessDeniedHandler(respuestasSeguridadJson))
            .authorizeHttpRequests(reglas -> reglas
                .requestMatchers(HttpMethod.POST, "/api/autenticacion/inicio-sesion").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt
                    .decoder(decodificador)
                    .jwtAuthenticationConverter(convertidor))
                .authenticationEntryPoint(respuestasSeguridadJson)
                .accessDeniedHandler(respuestasSeguridadJson));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource fuenteCors(
        @Value("${bankbatch.seguridad.cors.origenes-permitidos:http://localhost:4200}")
        List<String> origenes) {

        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(origenes);
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuracion.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);

        return fuente;
    }
}
