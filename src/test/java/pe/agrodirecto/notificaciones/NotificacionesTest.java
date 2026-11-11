package pe.agrodirecto.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import pe.agrodirecto.PruebaIntegracion;

/** RN-B2: el agricultor se entera por SMS/WhatsApp y responde desde cualquier celular. */
class NotificacionesTest extends PruebaIntegracion {

    private static final String SECRETO = "cambiar-en-produccion";
    private static final String CELULAR_ANDESVERDE = "900000004";

    // ---------- Avisos al agricultor ----------

    @Test
    void alPagarseElPedidoElAgricultorRecibeSmsYEnlaceDeWhatsapp() throws Exception {
        String codigo = codigo(pedidoPagado(1L, 2));
        Long andesVerde = idProductor("40000004");

        getCon(token(PRODUCTOR), "/api/gestion/mensajes?productorId=" + andesVerde)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.canal == 'SMS')].mensaje", hasItem(containsString("Recibiras S/ 261.00"))))
                .andExpect(jsonPath("$[?(@.canal == 'SMS')].mensaje", hasItem(containsString("Responde 1 " + codigo))))
                .andExpect(jsonPath("$[?(@.canal == 'WHATSAPP')].enlace",
                        hasItem(startsWith("https://wa.me/51900000004?text=AgroDirecto%3A%20nuevo%20pedido"))));

        // AndesVerde tiene cuenta propia: también lo ve en la campana de la web.
        getCon(token(PRODUCTOR), "/api/notificaciones")
                .andExpect(jsonPath("$.noLeidas").value(1))
                .andExpect(jsonPath("$.notificaciones[0].mensaje", containsString("Recibirás S/ 261.00")));
    }

    @Test
    void unPedidoSinPagarNoMolestaAlAgricultor() throws Exception {
        postCon(token(COMPRADOR), "/api/pedidos", pedidoJson(1L, 2)).andExpect(status().isCreated());
        nuevaPeticion();
        getCon(token(PRODUCTOR), "/api/gestion/mensajes?productorId=" + idProductor("40000004"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void elAgenteRecibeLosAvisosDeSusAgricultoresSinCuenta() throws Exception {
        Long tomate = jdbc.queryForObject("SELECT id FROM formato_venta WHERE producto_id = 2 AND nombre = 'Caja de 20 kg'",
                Long.class);
        pedidoPagado(tomate, 3);
        getCon(token(AGENTE), "/api/notificaciones")
                .andExpect(jsonPath("$.notificaciones[0].mensaje", startsWith("[ValleFresco] AgroDirecto: nuevo pedido")));
        getCon(token(AGENTE), "/api/gestion/mensajes?productorId=" + idProductor("40000011"))
                .andExpect(jsonPath("$.length()").value(2));
        // Otro productor no puede leer los mensajes de ValleFresco.
        getCon(token(PRODUCTOR), "/api/gestion/mensajes?productorId=" + idProductor("40000011"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elCompradorSigueSuPedidoEnLaCampanaYPuedeMarcarloLeido() throws Exception {
        Long id = pedidoPagado(1L, 2);
        postCon(token(PRODUCTOR), "/api/gestion/pedidos/" + id + "/aceptar", null).andExpect(status().isOk());
        nuevaPeticion();

        String comprador = token(COMPRADOR);
        String cuerpo = getCon(comprador, "/api/notificaciones")
                .andExpect(jsonPath("$.noLeidas").value(1))
                .andExpect(jsonPath("$.notificaciones[0].mensaje",
                        containsString("AndesVerde aceptó tu pedido")))
                .andReturn().getResponse().getContentAsString();
        Integer notificacion = JsonPath.read(cuerpo, "$.notificaciones[0].id");

        postCon(comprador, "/api/notificaciones/" + notificacion + "/leida", null).andExpect(status().isNoContent());
        nuevaPeticion();
        getCon(comprador, "/api/notificaciones").andExpect(jsonPath("$.noLeidas").value(0));
    }

    // ---------- Respuesta por SMS ----------

    @Test
    void elAgricultorAceptaRespondiendoUno() throws Exception {
        Long id = pedidoPagado(1L, 2);
        String codigo = codigo(id);

        sms(CELULAR_ANDESVERDE, "1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accion").value("ACEPTADO"))
                .andExpect(jsonPath("$.pedido").value(codigo))
                .andExpect(jsonPath("$.respuesta", containsString("Prepara: 2 x Saco de 50 kg de Papa amarilla")));

        nuevaPeticion();
        getCon(token(COMPRADOR), "/api/pedidos/" + id)
                .andExpect(jsonPath("$.estado").value("ACEPTADO"))
                .andExpect(jsonPath("$.historial[2].canal").value("SMS"))
                .andExpect(jsonPath("$.historial[2].usuario").doesNotExist());
    }

    @Test
    void entiendeElCodigoEscritoDeDistintasFormas() throws Exception {
        Long id = pedidoPagado(1L, 2);
        String numero = codigo(id).substring(3);
        sms("+51 900 000 004", "Sí ag " + numero).andExpect(jsonPath("$.accion").value("ACEPTADO"));
    }

    @Test
    void alRechazarPorSmsSeDevuelveElDineroYElStock() throws Exception {
        Long id = pedidoPagado(1L, 2);
        sms(CELULAR_ANDESVERDE, "2 " + codigo(id)).andExpect(jsonPath("$.accion").value("RECHAZADO"));
        nuevaPeticion();
        getCon(token(COMPRADOR), "/api/pedidos/" + id)
                .andExpect(jsonPath("$.estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.pago.estado").value("DEVUELTO"));
        assertThat(jdbc.queryForObject("SELECT stock FROM formato_venta WHERE id = 1", Integer.class)).isEqualTo(18);
    }

    @Test
    void conVariosPendientesPideElCodigo() throws Exception {
        String primero = codigo(pedidoPagado(1L, 2));
        String segundo = codigo(pedidoPagado(1L, 3));
        sms(CELULAR_ANDESVERDE, "1")
                .andExpect(jsonPath("$.accion").doesNotExist())
                .andExpect(jsonPath("$.respuesta", containsString("Tienes 2 pedidos por responder: " + primero + ", " + segundo)));
    }

    @Test
    void respondeConAyudaAnteMensajesNoEntendidosONumerosDesconocidos() throws Exception {
        sms(CELULAR_ANDESVERDE, "hola buenas").andExpect(jsonPath("$.respuesta", startsWith("No entendimos")));
        sms(CELULAR_ANDESVERDE, "1").andExpect(jsonPath("$.respuesta").value("No tienes pedidos por responder."));
        sms("911111111", "1").andExpect(jsonPath("$.respuesta", containsString("no esta registrado")));
    }

    @Test
    void elWebhookExigeElSecreto() throws Exception {
        mvc.perform(post("/api/sms/entrante").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefono\":\"900000004\",\"mensaje\":\"1\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/sms/entrante").header("X-Webhook-Secreto", "adivinado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefono\":\"900000004\",\"mensaje\":\"1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void alLiberarseElPagoElAgricultorRecibeConfirmacion() throws Exception {
        Long id = pedidoPagado(1L, 2);
        String productor = token(PRODUCTOR);
        postCon(productor, "/api/gestion/pedidos/" + id + "/aceptar", null);
        for (String estado : new String[] {"EN_PREPARACION", "LISTO_PARA_ENTREGA", "ENTREGADO"}) {
            postCon(productor, "/api/gestion/pedidos/" + id + "/estado", "{\"estado\":\"" + estado + "\"}");
        }
        postCon(token(COMPRADOR), "/api/pedidos/" + id + "/confirmar-recepcion", null).andExpect(status().isOk());
        nuevaPeticion();

        getCon(productor, "/api/gestion/mensajes?productorId=" + idProductor("40000004"))
                .andExpect(jsonPath("$[0].mensaje", containsString("pago liberado. S/ 261.00")))
                .andExpect(jsonPath("$[0].mensaje", containsString("YAPE terminado en 004")))
                .andExpect(jsonPath("$[*].mensaje", not(hasItem(containsString("cancel")))));
    }

    // ---------- Apoyo ----------

    private Long pedidoPagado(Long formatoId, int cantidad) throws Exception {
        String comprador = token(COMPRADOR);
        String cuerpo = postCon(comprador, "/api/pedidos", pedidoJson(formatoId, cantidad))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long id = ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
        postCon(comprador, "/api/pedidos/" + id + "/pago", "{\"metodo\":\"YAPE\"}").andExpect(status().isOk());
        nuevaPeticion();
        return id;
    }

    private String codigo(Long pedidoId) {
        return jdbc.queryForObject("SELECT codigo FROM pedido WHERE id = ?", String.class, pedidoId);
    }

    private ResultActions sms(String telefono, String mensaje) throws Exception {
        ResultActions resultado = mvc.perform(post("/api/sms/entrante").header("X-Webhook-Secreto", SECRETO)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefono\":\"" + telefono + "\",\"mensaje\":\"" + mensaje + "\"}"));
        nuevaPeticion();
        return resultado;
    }

    private static String pedidoJson(Long formatoId, int cantidad) {
        return """
                {"items":[{"formatoId":%d,"cantidad":%d}],"modalidadEntrega":"ENTREGA_DEL_PRODUCTOR",
                 "regionDestino":"Lima","direccionEntrega":"Av. Javier Prado 2450, San Borja"}"""
                .formatted(formatoId, cantidad);
    }
}
