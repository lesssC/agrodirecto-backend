package pe.agrodirecto.productores;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import pe.agrodirecto.PruebaIntegracion;

class ProductorControllerTest extends PruebaIntegracion {

    private static final String AGRICULTOR_NUEVO = """
            {"nombreComercial":"Chacra Doña Juana","nombreTitular":"Juana Ccori Huillca","dni":"41234567",
             "telefono":"912345678","region":"cusco","provincia":"Calca","distrito":"Pisac",
             "latitud":-13.4213,"longitud":-71.8521,"historia":"Cultiva maíz blanco gigante en terrazas.",
             "metodoCobro":"CUENTA_DNI","numeroCobro":"41234567"}""";

    // ---------- Directorio público ----------

    @Test
    void elDirectorioEsPublicoYNoExponeDatosPrivados() throws Exception {
        getCon(null, "/api/productores")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].nombreComercial").value("AgroSol"))
                .andExpect(jsonPath("$[0].dni").doesNotExist())
                .andExpect(jsonPath("$[0].telefono").doesNotExist())
                .andExpect(jsonPath("$[0].numeroCobro").doesNotExist());
    }

    @Test
    void laUbicacionPublicaEsAproximada() throws Exception {
        getCon(null, "/api/productores/" + idProductor("40000004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitud").value(-11.92))
                .andExpect(jsonPath("$.longitud").value(-75.31));
    }

    @Test
    void filtraPorRegionAtendidaSinImportarTildes() throws Exception {
        getCon(null, "/api/productores?atiende=cusco")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombreComercial").value("ValleFresco"));
        getCon(null, "/api/productores?region=JUNIN")
                .andExpect(jsonPath("$[0].nombreComercial").value("AndesVerde"));
        getCon(null, "/api/productores?atiende=Narnia").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void laCooperativaFacturaPorElSocioSinRuc() throws Exception {
        getCon(null, "/api/productores/" + idProductor("40000014"))
                .andExpect(jsonPath("$.emiteFactura").value(true))
                .andExpect(jsonPath("$.asociacion.nombre").value("Cooperativa Agraria Altura Puno"));
    }

    // ---------- Registro asistido por el agente ----------

    @Test
    void elAgenteRegistraAUnAgricultorSinCuenta() throws Exception {
        String agente = token(AGENTE);
        postCon(agente, "/api/gestion/productores/asistidos", AGRICULTOR_NUEVO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.region").value("Cusco"))
                .andExpect(jsonPath("$.tieneCuentaPropia").value(false))
                .andExpect(jsonPath("$.agente").value("Rosa Quispe Mamani"))
                .andExpect(jsonPath("$.verificado").value(false));

        getCon(agente, "/api/gestion/productores")
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[*].nombreComercial", hasItem("Chacra Doña Juana")));
    }

    @Test
    void unAgenteSinVerificarNoPuedeRegistrarAgricultores() throws Exception {
        postCon(null, "/api/auth/registro", """
                {"nombre":"Promotor Nuevo","email":"nuevo@agente.pe","password":"segura123",
                 "telefono":"955555555","dni":"45555555","rol":"AGENTE"}""").andExpect(status().isCreated());

        postCon(activarDobleFactor("nuevo@agente.pe", "segura123"), "/api/gestion/productores/asistidos", AGRICULTOR_NUEVO)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-17"));
    }

    @Test
    void unCompradorNoPuedeRegistrarAgricultores() throws Exception {
        postCon(token(COMPRADOR), "/api/gestion/productores/asistidos", AGRICULTOR_NUEVO)
                .andExpect(status().isForbidden());
    }

    @Test
    void noSePuedeRegistrarDosVecesAlMismoAgricultor() throws Exception {
        postCon(token(AGENTE), "/api/gestion/productores/asistidos",
                AGRICULTOR_NUEVO.replace("41234567", "40000011"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-23"));
    }

    @Test
    void validaElMedioDeCobroYLaRegion() throws Exception {
        String agente = token(AGENTE);
        postCon(agente, "/api/gestion/productores/asistidos",
                AGRICULTOR_NUEVO.replace("\"CUENTA_DNI\",\"numeroCobro\":\"41234567\"", "\"YAPE\",\"numeroCobro\":\"41234567\""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-C2"));
        postCon(agente, "/api/gestion/productores/asistidos", AGRICULTOR_NUEVO.replace("\"cusco\"", "\"Cuzco City\""))
                .andExpect(status().isUnprocessableContent());
        postCon(agente, "/api/gestion/productores/asistidos", AGRICULTOR_NUEVO.replace("-71.8521", "2.35"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-06"));
    }

    // ---------- Verificación presencial ----------

    @Test
    void elAgenteVerificaEnPersonaASuAgricultor() throws Exception {
        Long selvaNorte = idProductor("40000015");
        postCon(token(AGENTE), "/api/gestion/productores/" + selvaNorte + "/verificar", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificado").value(true))
                .andExpect(jsonPath("$.verificadoPor").value("Rosa Quispe Mamani"));
    }

    @Test
    void unProductorNoPuedeVerificarseASiMismo() throws Exception {
        postCon(token(PRODUCTOR), "/api/gestion/productores/" + idProductor("40000004") + "/verificar", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void cambiarElDniAnulaLaVerificacion() throws Exception {
        Long valleFresco = idProductor("40000011");
        String perfil = AGRICULTOR_NUEVO.replace("Chacra Doña Juana", "ValleFresco");
        putCon(token(AGENTE), "/api/gestion/productores/" + valleFresco, perfil)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").value("41234567"))
                .andExpect(jsonPath("$.verificado").value(false));
    }

    // ---------- Perfil propio y permisos ----------

    @Test
    void unProductorConCuentaCreaSuPerfilUnaSolaVez() throws Exception {
        postCon(null, "/api/auth/registro", """
                {"nombre":"Juana Ccori","email":"juana@chacra.pe","password":"segura123",
                 "telefono":"912345678","dni":"41234567","rol":"PRODUCTOR"}""").andExpect(status().isCreated());
        String juana = token("juana@chacra.pe", "segura123");

        postCon(juana, "/api/gestion/productores/mi-perfil", AGRICULTOR_NUEVO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tieneCuentaPropia").value(true));
        postCon(juana, "/api/gestion/productores/mi-perfil", AGRICULTOR_NUEVO)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-23"));
    }

    @Test
    void nadieEditaElPerfilDeUnAgricultorQueNoGestiona() throws Exception {
        // AndesVerde tiene cuenta propia y no lo gestiona la promotora.
        putCon(token(AGENTE), "/api/gestion/productores/" + idProductor("40000004"), AGRICULTOR_NUEVO)
                .andExpect(status().isForbidden());
        getCon(token(PRODUCTOR), "/api/gestion/productores/" + idProductor("40000011"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elProductorSoloVeSuPropioPerfilEnGestion() throws Exception {
        getCon(token(PRODUCTOR), "/api/gestion/productores")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombreComercial").value("AndesVerde"))
                .andExpect(jsonPath("$[0].numeroCobro").value("900000004"));
    }

    @Test
    void laGestionRequiereSesion() throws Exception {
        getCon(null, "/api/gestion/productores").andExpect(status().isUnauthorized());
    }

    // ---------- Zonas de cobertura ----------

    @Test
    void reemplazaLasZonasDeCobertura() throws Exception {
        Long andesVerde = idProductor("40000004");
        putCon(token(PRODUCTOR), "/api/gestion/productores/" + andesVerde + "/zonas", """
                [{"region":"Junín","diasTransito":0,"pedidoMinimo":50,"costoFlete":0},
                 {"region":"lima","diasTransito":1,"pedidoMinimo":150,"costoFlete":25},
                 {"region":"Huancavelica","diasTransito":1,"pedidoMinimo":100,"costoFlete":20}]""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zonas.length()").value(3))
                .andExpect(jsonPath("$.zonas[*].region", hasItem("Huancavelica")));

        nuevaPeticion();
        getCon(null, "/api/productores?atiende=Huancavelica")
                .andExpect(jsonPath("$[0].nombreComercial").value("AndesVerde"));
    }

    @Test
    void rechazaZonasRepetidasOInvalidas() throws Exception {
        String productor = token(PRODUCTOR);
        String url = "/api/gestion/productores/" + idProductor("40000004") + "/zonas";
        putCon(productor, url, """
                [{"region":"Lima","diasTransito":1,"pedidoMinimo":150,"costoFlete":25},
                 {"region":"LIMA","diasTransito":2,"pedidoMinimo":100,"costoFlete":20}]""")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-09"));
        putCon(productor, url, """
                [{"region":"Lima","diasTransito":-1,"pedidoMinimo":150,"costoFlete":25}]""")
                .andExpect(status().isBadRequest());
    }

    @Test
    void listaRegionesYAsociaciones() throws Exception {
        getCon(null, "/api/regiones").andExpect(jsonPath("$.length()").value(25));
        getCon(null, "/api/asociaciones").andExpect(jsonPath("$[0].emiteFactura").value(true));
        getCon(null, "/api/productores")
                .andExpect(jsonPath("$[*].nombreComercial", not(hasItem("Chacra Doña Juana"))));
    }
}
