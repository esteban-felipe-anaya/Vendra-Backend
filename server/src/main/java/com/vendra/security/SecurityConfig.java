package com.vendra.security;

import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless OAuth2 resource server. Public browse endpoints are open; everything else requires a
 * valid Supabase JWT and is further authorized per-method with {@code @PreAuthorize}.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtRoleConverter jwtRoleConverter;
  private final String[] allowedOrigins;

  public SecurityConfig(
      JwtRoleConverter jwtRoleConverter,
      @Value("${vendra.cors.allowed-origins}") String allowedOrigins) {
    this.jwtRoleConverter = jwtRoleConverter;
    this.allowedOrigins = allowedOrigins.split("\\s*,\\s*");
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(c -> c.configurationSource(corsConfigurationSource()))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth
                    // --- open: docs + health ---
                    .requestMatchers("/docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/actuator/health")
                    .permitAll()
                    // --- open: public catalog browse (GET only) ---
                    .requestMatchers(HttpMethod.GET, "/api/v1/catalog/**", "/api/v1/shops/**", "/api/v1/products/**", "/api/v1/categories/**")
                    .permitAll()
                    // --- auth endpoints issue/refresh tokens, so no JWT required ---
                    .requestMatchers("/api/v1/auth/**")
                    .permitAll()
                    // --- Stripe webhook authenticates by signature, not JWT ---
                    .requestMatchers("/api/v1/webhooks/stripe")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtRoleConverter)));
    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Arrays.asList(allowedOrigins));
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(Arrays.asList("*"));
    config.setExposedHeaders(Arrays.asList("X-Total-Count"));
    config.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
