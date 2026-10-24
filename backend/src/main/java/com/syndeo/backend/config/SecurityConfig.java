
package com.syndeo.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndeo.backend.dto.ApiErrorResponse;
import com.syndeo.backend.security.JwtAuthenticationFilter;
import com.syndeo.backend.service.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

@Configuration
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService,
            JdbcTemplate jdbcTemplate) {
        return new JwtAuthenticationFilter(jwtService, jdbcTemplate);
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter>
            jwtAuthenticationFilterRegistration(JwtAuthenticationFilter jwtFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtFilter);
        registration.setEnabled(false);
        return registration;
    }
    @Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
config.setAllowedOrigins(List.of(
        "https://maheendrahx.github.io",
        "http://localhost:8000",
        "http://127.0.0.1:8000",
        "http://localhost:5500",
        "http://127.0.0.1:5500",
        "https://syndeo-frontend.vercel.app"
));

    config.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    ));

    config.setAllowedHeaders(List.of(
            "Authorization", "Content-Type"
    ));

    UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

    source.registerCorsConfiguration("/api/**", config);
    return source;
}
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter,
            ObjectMapper objectMapper,
            Clock clock) throws Exception {

        http
          .cors(cors ->
                cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(
                                        response,
                                        objectMapper,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "UNAUTHORIZED",
                                        "Authentication is required.",
                                        clock))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(
                                        response,
                                        objectMapper,
                                        HttpServletResponse.SC_FORBIDDEN,
                                        "FORBIDDEN",
                                        "You do not have permission to access this resource.",
                                        clock)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/health",
                                "/api/health/**")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void writeError(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            int status,
            String code,
            String message,
            Clock clock) throws IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(
                response.getOutputStream(),
                new ApiErrorResponse(code, message, Instant.now(clock)));
    }
}
