package pe.agrodirecto.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import pe.agrodirecto.PruebaIntegracion;
import pe.agrodirecto.usuarios.seguridad.Totp;

/** Verificación en dos pasos (S26): TOTP obligatorio para administración y agentes. */
class DobleFactorTest extends PruebaIntegracion {

    private static final String NUEVO_AGENTE = """
            {"nombre":"Promotor Nuevo","email":"nuevo@agente.pe","password":"segura123",
             "telefono":"955555555","dni":"45555555","rol":"AGENTE"}""";

    // ---------- Inicio de sesión en dos pasos ----------

    @Test
    void conSoloLaContrasenaNoSeUsaLaApi() throws Exception {
        String temporal = tokenTemporal(ADMIN);
        getCon(temporal, "/api/admin/resumen").andExpect(status().isUnauthorized());
        getCon(temporal, "/api/auth/yo").andExpect(status().isUnauthorized());
    }

    @Test
    void conElCodigoDeLaAppSeCompletaElIngreso() throws Exception {
        verificar(tokenTemporal(ADMIN), "000000").andExpect(status().isUnauthorized());

        String cuerpo = verificar(tokenTemporal(ADMIN), codigoDe(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.dobleFactorActivo").value(true))
                .andReturn().getResponse().getContentAsString();
        getCon(JsonPath.read(cuerpo, "$.token"), "/api/admin/resumen").andExpect(status().isOk());
    }

    @Test
    void unCodigoYaUsadoNoSirveDosVeces() throws Exception {
        String codigo = codigoDe(ADMIN);
        verificar(tokenTemporal(ADMIN), codigo).andExpect(status().isOk());
        nuevaPeticion();
        verificar(tokenTemporal(ADMIN), codigo).andExpect(status().isUnauthorized());
    }

    @Test
    void probarCodigosAlAzarBloqueaLaCuenta() throws Exception {
        for (int i = 0; i < 5; i++) {
            verificar(tokenTemporal(ADMIN), String.format("%06d", i)).andExpect(status().isUnauthorized());
            nuevaPeticion();
        }
        // La cuenta completa queda bloqueada: ni siquiera el paso de la contraseña responde.
        postCon(null, "/api/auth/login", "{\"email\":\"" + ADMIN + "\",\"password\":\"" + CLAVE_DEMO + "\"}")
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void unTokenTemporalFalsificadoSeRechaza() throws Exception {
        verificar(tokenTemporal(ADMIN) + "x", codigoDe(ADMIN)).andExpect(status().isUnauthorized());
        // Tampoco sirve un token de acceso normal en lugar del temporal.
        verificar(token(PRODUCTOR), "123456").andExpect(status().isUnauthorized());
    }

    // ---------- Configuración obligatoria para agentes ----------

    @Test
    void unAgenteNuevoDebeConfigurarloAntesDeOperar() throws Exception {
        postCon(null, "/api/auth/registro", NUEVO_AGENTE).andExpect(status().isCreated());
        String inicial = tokenDirecto("nuevo@agente.pe", "segura123");

        getCon(inicial, "/api/gestion/productores")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.regla").value("SEG-2FA"));
        getCon(inicial, "/api/auth/yo").andExpect(status().isOk());
        getCon(inicial, "/api/auth/2fa").andExpect(jsonPath("$.obligatorio").value(true));

        String configuracion = postCon(inicial, "/api/auth/2fa/configurar", null)
                .andExpect(jsonPath("$.uri", startsWith("otpauth://totp/AgroDirecto%3Anuevo%40agente.pe?secret=")))
                .andReturn().getResponse().getContentAsString();
        String secreto = JsonPath.read(configuracion, "$.secreto");

        postCon(inicial, "/api/auth/2fa/activar", "{\"codigo\":\"000000\"}")
                .andExpect(status().isUnprocessableContent());
        String activacion = postCon(inicial, "/api/auth/2fa/activar",
                "{\"codigo\":\"" + Totp.codigoActual(secreto, Instant.now()) + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigosRespaldo.length()").value(8))
                .andReturn().getResponse().getContentAsString();
        nuevaPeticion();

        getCon(JsonPath.read(activacion, "$.sesion.token"), "/api/gestion/productores").andExpect(status().isOk());
        // El token anterior a la activación ya no basta.
        getCon(inicial, "/api/gestion/productores").andExpect(status().isUnauthorized());
    }

    @Test
    void losCodigosDeRespaldoSirvenUnaSolaVez() throws Exception {
        postCon(null, "/api/auth/registro", NUEVO_AGENTE).andExpect(status().isCreated());
        String inicial = tokenDirecto("nuevo@agente.pe", "segura123");
        String secreto = JsonPath.read(postCon(inicial, "/api/auth/2fa/configurar", null)
                .andReturn().getResponse().getContentAsString(), "$.secreto");
        List<String> respaldos = JsonPath.read(postCon(inicial, "/api/auth/2fa/activar",
                "{\"codigo\":\"" + Totp.codigoActual(secreto, Instant.now()) + "\"}")
                .andReturn().getResponse().getContentAsString(), "$.codigosRespaldo");
        nuevaPeticion();

        String perdiElCelular = respaldos.getFirst().toLowerCase();
        verificar(tokenTemporal("nuevo@agente.pe", "segura123"), perdiElCelular).andExpect(status().isOk());
        nuevaPeticion();
        verificar(tokenTemporal("nuevo@agente.pe", "segura123"), perdiElCelular).andExpect(status().isUnauthorized());
        getCon(token(ADMIN), "/api/admin/usuarios?rol=AGENTE").andExpect(status().isOk());
    }

    @Test
    void elSecretoSeGuardaCifrado() throws Exception {
        String guardado = jdbc.queryForObject("SELECT totp_secreto FROM usuario WHERE email = ?", String.class, ADMIN);
        assertThat(guardado).isNotBlank().doesNotContain(secretosTotp.get(ADMIN));
    }

    // ---------- Opcional para el resto ----------

    @Test
    void unCompradorPuedeActivarloYDesactivarlo() throws Exception {
        String comprador = activarDobleFactor(COMPRADOR, CLAVE_DEMO);
        nuevaPeticion();
        getCon(comprador, "/api/auth/2fa")
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.obligatorio").value(false));
        // El código de la activación ya se usó; se espera al siguiente intervalo simulando el de 30 s después.
        String siguiente = Totp.codigoActual(secretosTotp.get(COMPRADOR), Instant.now().plusSeconds(30));
        postCon(comprador, "/api/auth/2fa/desactivar", "{\"codigo\":\"" + siguiente + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void unAgenteNoPuedeDesactivarlo() throws Exception {
        String agente = token(AGENTE);
        String siguiente = Totp.codigoActual(secretosTotp.get(AGENTE), Instant.now().plusSeconds(30));
        postCon(agente, "/api/auth/2fa/desactivar", "{\"codigo\":\"" + siguiente + "\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Tu rol requiere la verificación en dos pasos."));
    }

    // ---------- Apoyo ----------

    private String tokenTemporal(String email) throws Exception {
        return tokenTemporal(email, CLAVE_DEMO);
    }

    private String tokenTemporal(String email, String password) throws Exception {
        String cuerpo = postCon(null, "/api/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
                .andExpect(jsonPath("$.requiereSegundoFactor").value(true))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.tokenTemporal");
    }

    /** Login sin segundo factor configurado: devuelve el token directamente. */
    private String tokenDirecto(String email, String password) throws Exception {
        return JsonPath.read(postCon(null, "/api/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
                .andReturn().getResponse().getContentAsString(), "$.token");
    }

    private ResultActions verificar(String tokenTemporal, String codigo) throws Exception {
        return postCon(null, "/api/auth/2fa/verificar",
                "{\"tokenTemporal\":\"" + tokenTemporal + "\",\"codigo\":\"" + codigo + "\"}");
    }

    private String codigoDe(String email) {
        return Totp.codigoActual(secretosTotp.get(email), Instant.now());
    }
}
