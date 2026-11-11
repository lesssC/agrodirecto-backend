package pe.agrodirecto.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI de la API (Swagger UI en {@code /swagger-ui.html}).
 * Para probar rutas protegidas: iniciar sesión en {@code POST /api/auth/login}, copiar el token
 * y pegarlo en el botón "Authorize".
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "jwt";

    @Bean
    OpenAPI documentacionAgroDirecto() {
        return new OpenAPI()
                .info(new Info()
                        .title("AgroDirecto API")
                        .version("1.0")
                        .description("""
                                Conecta a pequeños agricultores del Perú con compradores empresariales.
                                El agricultor recibe el 100% de su precio; el servicio de 5% lo paga quien compra.
                                Las rutas de catálogo y productores son públicas; el resto requiere el token JWT
                                de POST /api/auth/login (las cuentas de agente y administración piden además TOTP)."""))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
