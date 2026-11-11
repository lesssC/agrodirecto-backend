package pe.agrodirecto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import jakarta.persistence.EntityManager;

/**
 * Base de las pruebas de la API: levanta la aplicación con H2 y los datos demo.
 * Cada prueba corre en una transacción que se revierte al terminar, así no se afectan entre sí.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class PruebaIntegracion {

    protected static final String ADMIN = "admin@agrodirecto.pe";
    protected static final String COMPRADOR = "compras@saborandino.pe";
    protected static final String COMPRADOR_NUEVO = "donlucho@correo.pe";
    protected static final String PRODUCTOR = "andesverde@agrodirecto.pe";
    protected static final String AGENTE = "promotora@agrodirecto.pe";
    protected static final String CLAVE_DEMO = "demo1234";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected EntityManager entityManager;

    protected String token(String email) throws Exception {
        return token(email, CLAVE_DEMO);
    }

    protected String token(String email, String password) throws Exception {
        String cuerpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.token");
    }

    protected ResultActions getCon(String token, String url) throws Exception {
        var peticion = get(url);
        return mvc.perform(token == null ? peticion : peticion.header("Authorization", "Bearer " + token));
    }

    protected ResultActions postCon(String token, String url, String json) throws Exception {
        var peticion = post(url).contentType(MediaType.APPLICATION_JSON).content(json == null ? "" : json);
        return mvc.perform(token == null ? peticion : peticion.header("Authorization", "Bearer " + token));
    }

    protected ResultActions putCon(String token, String url, String json) throws Exception {
        var peticion = put(url).contentType(MediaType.APPLICATION_JSON).content(json);
        return mvc.perform(token == null ? peticion : peticion.header("Authorization", "Bearer " + token));
    }

    protected Long idProductor(String dni) {
        return jdbc.queryForObject("SELECT id FROM productor WHERE dni = ?", Long.class, dni);
    }

    /** Simula que lo siguiente ocurre en otra petición: guarda los cambios pendientes y vacía la caché JPA. */
    protected void nuevaPeticion() {
        entityManager.flush();
        entityManager.clear();
    }
}
