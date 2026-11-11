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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;

import jakarta.persistence.EntityManager;
import pe.agrodirecto.usuarios.seguridad.Totp;
import pe.agrodirecto.usuarios.servicio.DobleFactorDemo;

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

    /** Secretos TOTP conocidos en esta prueba: los de las cuentas demo y los que la prueba active. */
    protected final Map<String, String> secretosTotp = new HashMap<>(DobleFactorDemo.SECRETOS);

    /**
     * Un código TOTP solo vale una vez, así que cada cuenta inicia sesión una sola vez por prueba
     * y el token se reutiliza.
     */
    private final Map<String, String> tokens = new HashMap<>();

    protected String token(String email) throws Exception {
        return token(email, CLAVE_DEMO);
    }

    /** Inicia sesión; si la cuenta tiene segundo factor, genera el código como lo haría la app del celular. */
    protected String token(String email, String password) throws Exception {
        String cacheado = tokens.get(email);
        if (cacheado != null) {
            return cacheado;
        }
        String cuerpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token;
        if (Boolean.TRUE.equals(JsonPath.read(cuerpo, "$.requiereSegundoFactor"))) {
            String temporal = JsonPath.read(cuerpo, "$.tokenTemporal");
            String codigo = Totp.codigoActual(secretosTotp.get(email), Instant.now());
            String verificado = mvc.perform(post("/api/auth/2fa/verificar").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tokenTemporal\":\"" + temporal + "\",\"codigo\":\"" + codigo + "\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            token = JsonPath.read(verificado, "$.token");
        } else {
            token = JsonPath.read(cuerpo, "$.token");
        }
        tokens.put(email, token);
        return token;
    }

    /**
     * Activa el segundo factor de una cuenta recién creada (agentes y administradores lo necesitan)
     * y devuelve su token con el segundo factor ya verificado.
     */
    protected String activarDobleFactor(String email, String password) throws Exception {
        String cuerpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getContentAsString();
        String inicial = JsonPath.read(cuerpo, "$.token");
        String configuracion = postCon(inicial, "/api/auth/2fa/configurar", null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String secreto = JsonPath.read(configuracion, "$.secreto");
        secretosTotp.put(email, secreto);
        String activacion = postCon(inicial, "/api/auth/2fa/activar",
                "{\"codigo\":\"" + Totp.codigoActual(secreto, Instant.now()) + "\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(activacion, "$.sesion.token");
        tokens.put(email, token);
        return token;
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
