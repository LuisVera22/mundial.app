package com.wirbi.mundial.config;

import com.azure.spring.cloud.autoconfigure.implementation.aad.security.AadResourceServerHttpSecurityConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad del API según {@code app.auth.enabled}:
 *  - true  → resource server con Microsoft Entra ID: todo /api exige un JWT
 *            válido del tenant (emitido para esta app). Stateless, sin sesiones.
 *  - false → modo desarrollo: todo abierto (el usuario se resuelve con el stub).
 * CORS se delega a la configuración MVC ({@link CorsConfig}) en ambos modos.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(name = "app.auth.enabled", havingValue = "true")
    SecurityFilterChain protectedApi(HttpSecurity http) throws Exception {
        http.with(AadResourceServerHttpSecurityConfigurer.aadResourceServer(), Customizer.withDefaults());
        http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // El cron de sync no puede traer JWT de Entra: el endpoint
                        // valida su propio secreto (X-Sync-Token).
                        .requestMatchers("/internal/sync").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.auth.enabled", havingValue = "false", matchIfMissing = true)
    SecurityFilterChain openApi(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
