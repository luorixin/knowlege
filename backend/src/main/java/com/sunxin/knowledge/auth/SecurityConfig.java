package com.sunxin.knowledge.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${knowledge.security.enabled:true}")
    private boolean securityEnabled;

    @Value("${knowledge.jwt.secret}")
    private String jwtSecret;

    @Value("${knowledge.security.cors.allowed-origin-patterns:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOriginPatterns;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        if (securityEnabled) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/**", "/api/v1/health").permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("/api/v1/auth/login").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().permitAll()
            ).oauth2ResourceServer(oauth2 -> oauth2.jwt(org.springframework.security.config.Customizer.withDefaults()));
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = buildCorsConfiguration(allowedOriginPatterns);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    static CorsConfiguration buildCorsConfiguration(String allowedOriginPatterns) {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream((allowedOriginPatterns == null ? "" : allowedOriginPatterns).split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
        configuration.setAllowedOriginPatterns(origins.isEmpty()
                ? List.of("http://localhost:5173", "http://127.0.0.1:5173")
                : origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        return configuration;
    }

    @Bean
    public org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder() {
        javax.crypto.SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(jwtSecret.getBytes(), "HMACSHA256");
        return org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
    }

    @Bean
    public org.springframework.security.oauth2.jwt.JwtEncoder jwtEncoder() {
        javax.crypto.SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(jwtSecret.getBytes(), "HMACSHA256");
        com.nimbusds.jose.jwk.source.JWKSource<com.nimbusds.jose.proc.SecurityContext> immutableSecret = 
                new com.nimbusds.jose.jwk.source.ImmutableSecret<>(secretKey);
        return new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(immutableSecret);
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }
}
