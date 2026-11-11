package pe.agrodirecto.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import pe.agrodirecto.PruebaIntegracion;

/** La documentación de la API es pública y declara el esquema JWT para las rutas protegidas. */
class DocumentacionApiTest extends PruebaIntegracion {

    @Test
    void laEspecificacionOpenApiEsPublicaYDocumentaLasRutas() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("AgroDirecto API"))
                .andExpect(jsonPath("$.components.securitySchemes.jwt.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/pedidos/cotizacion']").exists());
    }

    @Test
    void swaggerUiEstaDisponibleSinIniciarSesion() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
