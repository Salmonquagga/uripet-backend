package com.dbp.uripet.config.jwt;

import com.dbp.uripet.config.error.RestAccessDeniedHandler;
import com.dbp.uripet.config.error.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter
            jwtAuthFilter;

    private final AccountVerificationFilter
            accountVerificationFilter;

    private final AuthenticationProvider
            authenticationProvider;

    private final RestAuthenticationEntryPoint
            restAuthenticationEntryPoint;

    private final RestAccessDeniedHandler
            restAccessDeniedHandler;

    @Value(
            "${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}"
    )
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain
    securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .cors(Customizer.withDefaults())

                .csrf(
                        AbstractHttpConfigurer::disable
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth ->
                        auth
                                /*
                                 * Autenticación y verificación.
                                 */
                                .requestMatchers(
                                        "/auth/**"
                                )
                                .permitAll()

                                /*
                                 * Información pública de mascotas.
                                 */
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/pets/public/**"
                                )
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/pets/*/qr-data"
                                )
                                .permitAll()

                                /*
                                 * Catálogo público de planes.
                                 */
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/plans",
                                        "/plans/**"
                                )
                                .permitAll()

                                /*
                                 * El webhook no utiliza JWT.
                                 * Se protege con X-Webhook-Secret.
                                 */
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/billing/webhook"
                                )
                                .permitAll()

                                /*
                                 * Revisión de negocios solo para ADMIN.
                                 */
                                .requestMatchers(
                                        "/business-verifications/admin/**"
                                )
                                .hasRole("ADMIN")

                                /*
                                 * Swagger.
                                 * Al final podremos desactivarlo por variable
                                 * sin cambiar este archivo.
                                 */
                                .requestMatchers(
                                        "/v3/api-docs/**",
                                        "/api/v3/api-docs/**",
                                        "/api/docs",
                                        "/api/docs/**",
                                        "/swagger-ui/**",
                                        "/api/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/api/swagger-ui.html"
                                )
                                .permitAll()

                                /*
                                 * Todo lo demás requiere JWT.
                                 */
                                .anyRequest()
                                .authenticated()
                )

                .authenticationProvider(
                        authenticationProvider
                )

                .exceptionHandling(exception ->
                        exception
                                .authenticationEntryPoint(
                                        restAuthenticationEntryPoint
                                )
                                .accessDeniedHandler(
                                        restAccessDeniedHandler
                                )
                )

                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .addFilterAfter(
                        accountVerificationFilter,
                        JwtAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource
    corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        List<String> origins =
                Arrays.stream(
                                allowedOrigins.split(",")
                        )
                        .map(String::trim)
                        .filter(origin ->
                                !origin.isBlank()
                        )
                        .toList();

        configuration.setAllowedOrigins(
                origins
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        HttpHeaders.AUTHORIZATION,
                        HttpHeaders.CONTENT_TYPE,
                        HttpHeaders.ACCEPT,
                        "X-Webhook-Secret"
                )
        );

        configuration.setExposedHeaders(
                List.of(
                        HttpHeaders.AUTHORIZATION
                )
        );

        configuration.setAllowCredentials(
                true
        );

        configuration.setMaxAge(
                3600L
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}