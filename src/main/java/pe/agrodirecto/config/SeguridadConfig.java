package pe.agrodirecto.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Seguridad sin estado: cada petición se autentica con un JWT firmado (HS256) en la cabecera
 * {@code Authorization: Bearer <token>}. El rol viaja en el claim {@code rol} y se convierte en {@code ROLE_<rol>}.
 */
@Configuration
public class SeguridadConfig {

    public static final String CLAIM_ROL = "rol";

    @Bean
    SecurityFilterChain filtroSeguridad(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/registro", "/api/auth/login").permitAll()
                        // Catálogo y directorio de productores son públicos (cualquier visitante puede explorar).
                        .requestMatchers(HttpMethod.GET, "/api/productos/**", "/api/categorias/**",
                                "/api/productores/**", "/api/precios-referencia/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(servidor -> servidor
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorRoles()))
                        .authenticationEntryPoint(sinAutenticar()))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(sinAutenticar())
                        .accessDeniedHandler(sinPermiso()));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder(AgroDirectoProperties propiedades) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave(propiedades)));
    }

    @Bean
    JwtDecoder jwtDecoder(AgroDirectoProperties propiedades) {
        return NimbusJwtDecoder.withSecretKey(clave(propiedades)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AgroDirectoProperties propiedades) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOriginPatterns(propiedades.origenesPermitidos());
        cors.addAllowedMethod("*");
        cors.addAllowedHeader("*");
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", cors);
        return fuente;
    }

    private JwtAuthenticationConverter convertidorRoles() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(CLAIM_ROL);
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(roles);
        return convertidor;
    }

    private static SecretKey clave(AgroDirectoProperties propiedades) {
        byte[] bytes = propiedades.seguridad().jwtSecreto().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("agrodirecto.seguridad.jwt-secreto debe tener al menos 32 caracteres.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    private static AuthenticationEntryPoint sinAutenticar() {
        return (request, response, e) -> escribirError(response, 401, null,
                "Debes iniciar sesión o tu sesión expiró.");
    }

    private static AccessDeniedHandler sinPermiso() {
        return (request, response, e) -> escribirError(response, 403, "RN-07",
                "Tu rol no tiene permiso para esta operación.");
    }

    /** Mismo formato que {@code ErrorRespuesta}, escrito a mano porque ocurre antes de llegar a los controladores. */
    private static void escribirError(jakarta.servlet.http.HttpServletResponse response, int estado, String regla,
            String mensaje) throws java.io.IOException {
        response.setStatus(estado);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String reglaJson = regla == null ? "null" : "\"" + regla + "\"";
        response.getWriter().write("{\"estado\":" + estado + ",\"regla\":" + reglaJson + ",\"mensaje\":\"" + mensaje
                + "\",\"detalles\":[],\"fecha\":\"" + java.time.LocalDateTime.now(AplicacionConfig.ZONA_PERU) + "\"}");
    }
}
