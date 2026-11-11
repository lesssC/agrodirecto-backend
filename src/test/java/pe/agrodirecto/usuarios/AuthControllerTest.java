package pe.agrodirecto.usuarios;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import pe.agrodirecto.PruebaIntegracion;

class AuthControllerTest extends PruebaIntegracion {

    @Test
    void registraUnCompradorEIniciaSesion() throws Exception {
        registrar("""
                {"nombre":"Cevichería La Ola","email":"Compras@LaOla.pe","password":"segura123",
                 "telefono":"987654321","ruc":"20123456789","razonSocial":"La Ola S.A.C.","rol":"COMPRADOR"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("compras@laola.pe"))
                .andExpect(jsonPath("$.rol").value("COMPRADOR"))
                .andExpect(jsonPath("$.verificado").value(false))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        String token = login("compras@laola.pe", "segura123");

        mvc.perform(get("/api/auth/yo").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Cevichería La Ola"));
    }

    @Test
    void lasCuentasDemoInicianSesionConSuRol() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"promotora@agrodirecto.pe\",\"password\":\"demo1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuario.rol").value("AGENTE"));
    }

    @Test
    void sinTokenNoSePuedeConsultarLaCuenta() throws Exception {
        mvc.perform(get("/api/auth/yo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void rechazaUnTokenAlterado() throws Exception {
        String token = login("compras@saborandino.pe", "demo1234");
        mvc.perform(get("/api/auth/yo").header("Authorization", "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void soloElAdministradorEntraALaZonaAdmin() throws Exception {
        String comprador = login("compras@saborandino.pe", "demo1234");
        mvc.perform(get("/api/admin/cualquier-cosa").header("Authorization", "Bearer " + comprador))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.regla").value("RN-07"));
    }

    @Test
    void nadiePuedeRegistrarseComoAdministrador() throws Exception {
        registrar("""
                {"nombre":"Intruso","email":"x@x.pe","password":"segura123","telefono":"911111111","rol":"ADMINISTRADOR"}""")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-07"));
    }

    @Test
    void elAgenteDebeRegistrarSuDni() throws Exception {
        registrar("""
                {"nombre":"Promotor","email":"p@x.pe","password":"segura123","telefono":"911111112","rol":"AGENTE"}""")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-17"));
    }

    @Test
    void noPermiteCuentasDuplicadas() throws Exception {
        registrar("""
                {"nombre":"Otra","email":"COMPRAS@saborandino.pe","password":"segura123","telefono":"911111113","rol":"COMPRADOR"}""")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-23"));
        registrar("""
                {"nombre":"Otra","email":"nueva@x.pe","password":"segura123","telefono":"900000002","rol":"COMPRADOR"}""")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Ya existe una cuenta con ese número de celular."));
    }

    @Test
    void validaElFormatoDeLosDatos() throws Exception {
        registrar("""
                {"nombre":"","email":"no-es-correo","password":"corta","telefono":"12345","rol":"COMPRADOR"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.length()").value(4));
    }

    @Test
    void bloqueaLaCuentaTrasCincoIntentosFallidos() throws Exception {
        for (int i = 0; i < 5; i++) {
            intentarLogin("donlucho@correo.pe", "incorrecta").andExpect(status().isUnauthorized());
        }
        // Ni siquiera la contraseña correcta entra mientras dura el bloqueo.
        intentarLogin("donlucho@correo.pe", "demo1234").andExpect(status().isTooManyRequests());
    }

    @Test
    void unaCuentaSuspendidaQuedaFueraAunqueTengaTokenVigente() throws Exception {
        String token = login("donlucho@correo.pe", "demo1234");
        // Cada petición real abre su propio contexto JPA; aquí se simula vaciando la caché de la transacción de prueba.
        entityManager.flush();
        jdbc.update("UPDATE usuario SET estado = 'SUSPENDIDO' WHERE email = 'donlucho@correo.pe'");
        entityManager.clear();

        mvc.perform(get("/api/auth/yo").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.regla").value("RN-23"));
        intentarLogin("donlucho@correo.pe", "demo1234")
                .andExpect(status().isForbidden());
    }

    private ResultActions registrar(String json) throws Exception {
        return mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions intentarLogin(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private String login(String email, String password) throws Exception {
        return token(email, password);
    }
}
