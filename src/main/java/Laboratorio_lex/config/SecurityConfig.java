package Laboratorio_lex.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final Environment environment;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean isDev = environment.acceptsProfiles("dev");

        http
                // 1. Deshabilitar CSRF y habilitar CORS
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. Permitir que H2 se renderice en iFrames (mismo origen) - solo en dev
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

                // 3. Política de sesión Stateless (JWT) + sin login de navegador
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // 4. Respuestas JSON ante fallos de autenticación/autorización (NF-15)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(
                                    "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Debe iniciar sesión para acceder a este recurso.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(
                                    "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"No tiene permisos para realizar esta acción.\"}");
                        }))

                // 5. RBAC: permisos diferenciados por rol (F-06)
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos de autenticación, portal público y documentación
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/recuperar-password", "/api/auth/reset-password").permitAll()
                        .requestMatchers("/api/publico/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // H2 Console solo en perfil dev
                        .requestMatchers("/h2-console/**").permitAll()

                        // Gestión de roles: consulta permitida para usuarios autenticados
                        .requestMatchers(HttpMethod.GET, "/api/catalogos/roles/**", "/api/auth/roles/**").hasAnyRole("ADMINISTRADOR", "GESTOR_PERSONAL", "SUPERVISOR_ACCESOS")

                        // Gestión de usuarios internos: solo Administrador (F-07, F-09, F-10)
                        .requestMatchers("/api/auth/usuarios/**").hasRole("ADMINISTRADOR")

                        // Auditoría: Administrador y Supervisor (F-33)
                        .requestMatchers("/api/auditoria/**").hasAnyRole("ADMINISTRADOR", "SUPERVISOR_ACCESOS")

                        // Sincronizacion con el socio: Administrador y Supervisor (F-30)
                        .requestMatchers("/api/sincronizacion/**").hasAnyRole("ADMINISTRADOR", "SUPERVISOR_ACCESOS")

                        // Autorizaciones de zona: Administrador y Gestor de Personal (F-19, F-21)
                        .requestMatchers("/api/accesos/autorizaciones/**").hasAnyRole("ADMINISTRADOR", "GESTOR_PERSONAL")

                        // Gestión de personal y catálogos (F-11 a F-19)
                        .requestMatchers("/api/personal/**").hasAnyRole("ADMINISTRADOR", "GESTOR_PERSONAL")

                        // Catálogos: lectura para gestión/supervisión, escritura solo Administrador (F-18)
                        .requestMatchers(HttpMethod.GET, "/api/catalogos/**").hasAnyRole("ADMINISTRADOR",
                                "GESTOR_PERSONAL", "SUPERVISOR_ACCESOS")
                        .requestMatchers("/api/catalogos/**").hasRole("ADMINISTRADOR")

                        // Simulación e historial de accesos (F-20 a F-25)
                        .requestMatchers("/api/accesos/**").hasAnyRole("ADMINISTRADOR", "GESTOR_PERSONAL",
                                "SUPERVISOR_ACCESOS")

                        // Cualquier otra ruta requiere Token Bearer válido
                        .anyRequest().authenticated())

                // 6. Registrar filtro JWT antes del filtro por defecto de Spring
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        String frontendUrl = environment.getProperty("FRONTEND_URL");
        if (frontendUrl == null || frontendUrl.isBlank()) {
            throw new IllegalStateException("FRONTEND_URL environment variable is required");
        }

        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOrigins(java.util.List.of(frontendUrl));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setAllowCredentials(true);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    @Profile("dev")
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSourceDev() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOriginPatterns(java.util.List.of("*"));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setAllowCredentials(true);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
