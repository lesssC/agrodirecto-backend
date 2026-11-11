package pe.agrodirecto.administracion;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.JsonPath;

import pe.agrodirecto.PruebaIntegracion;

class AdministracionTest extends PruebaIntegracion {

    private static final String MOTIVO = "{\"motivo\":\"DNI y RUC validados en SUNAT\"}";
    private static final String PEDIDO_PAPA = """
            {"items":[{"formatoId":1,"cantidad":%d}],"modalidadEntrega":"ENTREGA_DEL_PRODUCTOR",
             "regionDestino":"Lima","direccionEntrega":"Av. Javier Prado 2450, San Borja"}""";

    // ---------- Acceso ----------

    @Test
    void soloLaAdministracionEntraAlPanel() throws Exception {
        getCon(token(COMPRADOR), "/api/admin/resumen").andExpect(status().isForbidden());
        postCon(token(AGENTE), "/api/admin/usuarios/" + idUsuario(COMPRADOR_NUEVO) + "/suspender", MOTIVO)
                .andExpect(status().isForbidden());
        getCon(null, "/api/admin/resumen").andExpect(status().isUnauthorized());
    }

    // ---------- Métricas ----------

    @Test
    void elResumenMuestraElEstadoDeLaPlataforma() throws Exception {
        getCon(token(ADMIN), "/api/admin/resumen")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuariosPorRol.COMPRADOR").value(2))
                .andExpect(jsonPath("$.pendientesDeVerificacion.compradores").value(1))
                .andExpect(jsonPath("$.pendientesDeVerificacion.productores").value(1))
                .andExpect(jsonPath("$.productores").value(6))
                .andExpect(jsonPath("$.productoresVerificados").value(5))
                .andExpect(jsonPath("$.productosActivos").value(16))
                .andExpect(jsonPath("$.dinero.transado").value(0))
                .andExpect(jsonPath("$.dinero.porcentajeAlAgricultor").doesNotExist());
    }

    @Test
    void elResumenMideCuantoLlegaAlAgricultor() throws Exception {
        String comprador = token(COMPRADOR);
        completar(comprador, pagar(comprador, pedir(comprador, 2)));
        pagar(comprador, pedir(comprador, 2)); // otro pedido pagado y aún en curso

        getCon(token(ADMIN), "/api/admin/resumen")
                .andExpect(jsonPath("$.pedidosPorEstado.COMPLETADO").value(1))
                .andExpect(jsonPath("$.pedidosPorEstado.PENDIENTE").value(1))
                .andExpect(jsonPath("$.dinero.transado").value(272.80))
                .andExpect(jsonPath("$.dinero.pagadoAgricultores").value(261.00))
                .andExpect(jsonPath("$.dinero.comisiones").value(11.80))
                .andExpect(jsonPath("$.dinero.retenido").value(272.80))
                .andExpect(jsonPath("$.dinero.porcentajeAlAgricultor").value(95.7))
                .andExpect(jsonPath("$.topProductores[0].nombreComercial").value("AndesVerde"))
                .andExpect(jsonPath("$.topProductores[0].ventas").value(261.00));
    }

    // ---------- Verificación y sanciones ----------

    @Test
    void verificarAUnCompradorLeQuitaLosTopesDeCuentaNueva() throws Exception {
        String nuevo = token(COMPRADOR_NUEVO);
        postCon(nuevo, "/api/pedidos", PEDIDO_PAPA.formatted(13)).andExpect(status().isUnprocessableContent());

        postCon(token(ADMIN), "/api/admin/usuarios/" + idUsuario(COMPRADOR_NUEVO) + "/verificar", MOTIVO)
                .andExpect(jsonPath("$.verificado").value(true));
        nuevaPeticion();
        postCon(nuevo, "/api/pedidos", PEDIDO_PAPA.formatted(13)).andExpect(status().isCreated());
    }

    @Test
    void losAgricultoresSeVerificanEnPersonaNoDesdeElPanel() throws Exception {
        postCon(token(ADMIN), "/api/admin/usuarios/" + idUsuario(PRODUCTOR) + "/verificar", MOTIVO)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-17"));
    }

    @Test
    void laSuspensionCortaElAccesoYQuedaEnLaAuditoria() throws Exception {
        String admin = token(ADMIN);
        Long donLucho = idUsuario(COMPRADOR_NUEVO);
        String tokenPrevio = token(COMPRADOR_NUEVO);

        postCon(admin, "/api/admin/usuarios/" + donLucho + "/suspender", "{\"motivo\":\"Tres pedidos sin recoger\"}")
                .andExpect(jsonPath("$.estado").value("SUSPENDIDO"));
        nuevaPeticion();
        getCon(tokenPrevio, "/api/auth/yo").andExpect(status().isForbidden());

        postCon(admin, "/api/admin/usuarios/" + donLucho + "/reactivar", "{\"motivo\":\"Aclaró lo ocurrido\"}")
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
        nuevaPeticion();
        getCon(tokenPrevio, "/api/auth/yo").andExpect(status().isOk());

        getCon(admin, "/api/admin/auditoria?objetivo=USUARIO&objetivoId=" + donLucho)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].tipo").value("SUSPENDER_USUARIO"))
                .andExpect(jsonPath("$[1].detalle").value("Tres pedidos sin recoger"))
                .andExpect(jsonPath("$[1].administrador").value("Administración AgroDirecto"));
    }

    @Test
    void todaSancionNecesitaMotivoYNoSeSuspendeALaAdministracion() throws Exception {
        String admin = token(ADMIN);
        postCon(admin, "/api/admin/usuarios/" + idUsuario(COMPRADOR_NUEVO) + "/suspender", "{\"motivo\":\"\"}")
                .andExpect(status().isBadRequest());
        postCon(admin, "/api/admin/usuarios/" + idUsuario(ADMIN) + "/suspender", MOTIVO)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-07"));
    }

    @Test
    void unProductorSuspendidoSaleDelCatalogoHastaReactivarse() throws Exception {
        String admin = token(ADMIN);
        Long andesVerde = idProductor("40000004");
        postCon(admin, "/api/admin/productores/" + andesVerde + "/suspender", "{\"motivo\":\"Producto en mal estado\"}")
                .andExpect(jsonPath("$.estado").value("SUSPENDIDO"));
        nuevaPeticion();

        getCon(null, "/api/productos").andExpect(jsonPath("$.length()").value(13));
        getCon(null, "/api/productores").andExpect(jsonPath("$[*].nombreComercial", not(hasItem("AndesVerde"))));
        postCon(token(COMPRADOR), "/api/pedidos", PEDIDO_PAPA.formatted(2))
                .andExpect(status().isUnprocessableContent());

        postCon(admin, "/api/admin/productores/" + andesVerde + "/reactivar", "{\"motivo\":\"Corrigió el empaque\"}");
        nuevaPeticion();
        getCon(null, "/api/productos").andExpect(jsonPath("$.length()").value(16));
    }

    @Test
    void siSuAgenteFallaElAgricultorPasaAOtroSinPerderSuVitrina() throws Exception {
        String admin = token(ADMIN);
        postCon(null, "/api/auth/registro", """
                {"nombre":"Nuevo Promotor","email":"nuevo@agente.pe","password":"segura123",
                 "telefono":"955555555","dni":"45555555","rol":"AGENTE"}""").andExpect(status().isCreated());
        Long nuevoAgente = idUsuario("nuevo@agente.pe");
        Long valleFresco = idProductor("40000011");

        String reasignar = "{\"agenteId\":" + nuevoAgente + ",\"motivo\":\"La promotora anterior fue suspendida\"}";
        postCon(admin, "/api/admin/productores/" + valleFresco + "/agente", reasignar)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-B1"));

        postCon(admin, "/api/admin/usuarios/" + nuevoAgente + "/verificar", MOTIVO).andExpect(status().isOk());
        postCon(admin, "/api/admin/productores/" + valleFresco + "/agente", reasignar)
                .andExpect(jsonPath("$.agente").value("Nuevo Promotor"));
        nuevaPeticion();

        getCon(activarDobleFactor("nuevo@agente.pe", "segura123"), "/api/gestion/productores")
                .andExpect(jsonPath("$[0].nombreComercial").value("ValleFresco"));
        getCon(token(AGENTE), "/api/gestion/productores")
                .andExpect(jsonPath("$[*].nombreComercial", not(hasItem("ValleFresco"))));
        getCon(admin, "/api/admin/auditoria?objetivo=PRODUCTOR&objetivoId=" + valleFresco)
                .andExpect(jsonPath("$[0].detalle", containsString("Rosa Quispe Mamani → Nuevo Promotor")));
    }

    // ---------- Precios de referencia ----------

    @Test
    void actualizarElPrecioDeReferenciaRecalculaLasAlertasDelAgricultor() throws Exception {
        getCon(token(PRODUCTOR), "/api/gestion/productos/1").andExpect(jsonPath("$.alertas.length()").value(1));

        putCon(token(ADMIN), "/api/admin/precios-referencia/1", precio("PAPA_AMARILLA", "2.00", "3.80"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioMayoristaKg").value(2.00));
        nuevaPeticion();

        getCon(token(PRODUCTOR), "/api/gestion/productos/1").andExpect(jsonPath("$.alertas.length()").value(0));
        getCon(token(ADMIN), "/api/admin/auditoria")
                .andExpect(jsonPath("$[0].tipo").value("ACTUALIZAR_PRECIO_REFERENCIA"));
    }

    @Test
    void validaLosPreciosDeReferencia() throws Exception {
        String admin = token(ADMIN);
        postCon(admin, "/api/admin/precios-referencia", precio("MAIZ_MORADO", "3.50", "6.00"))
                .andExpect(status().isCreated());
        postCon(admin, "/api/admin/precios-referencia", precio("MAIZ_MORADO", "3.50", "6.00"))
                .andExpect(status().isUnprocessableContent());
        postCon(admin, "/api/admin/precios-referencia", precio("OCA", "4.00", "3.00"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje", containsString("minorista no puede ser menor")));
        putCon(admin, "/api/admin/precios-referencia/1", precio("OTRA_CLAVE", "2.00", "3.80"))
                .andExpect(status().isUnprocessableContent());
        nuevaPeticion();
        getCon(null, "/api/precios-referencia").andExpect(jsonPath("$.length()").value(11));
    }

    // ---------- Apoyo ----------

    private Long idUsuario(String email) {
        return jdbc.queryForObject("SELECT id FROM usuario WHERE email = ?", Long.class, email);
    }

    private Long pedir(String comprador, int sacos) throws Exception {
        String cuerpo = postCon(comprador, "/api/pedidos", PEDIDO_PAPA.formatted(sacos))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }

    private Long pagar(String comprador, Long id) throws Exception {
        postCon(comprador, "/api/pedidos/" + id + "/pago", "{\"metodo\":\"YAPE\"}").andExpect(status().isOk());
        nuevaPeticion();
        return id;
    }

    private void completar(String comprador, Long id) throws Exception {
        String productor = token(PRODUCTOR);
        postCon(productor, "/api/gestion/pedidos/" + id + "/aceptar", null).andExpect(status().isOk());
        for (String estado : new String[] {"EN_PREPARACION", "LISTO_PARA_ENTREGA", "ENTREGADO"}) {
            postCon(productor, "/api/gestion/pedidos/" + id + "/estado", "{\"estado\":\"" + estado + "\"}");
        }
        postCon(comprador, "/api/pedidos/" + id + "/confirmar-recepcion", null).andExpect(status().isOk());
        nuevaPeticion();
    }

    private static String precio(String clave, String mayorista, String minorista) {
        return """
                {"clave":"%s","nombre":"%s","precioMayoristaKg":%s,"precioMinoristaKg":%s,
                 "fuente":"Boletín de precios mayoristas","fecha":"2026-11-10"}"""
                .formatted(clave, clave.replace('_', ' ').toLowerCase(), mayorista, minorista);
    }
}
