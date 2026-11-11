package pe.agrodirecto.confianza;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import pe.agrodirecto.PruebaIntegracion;
import pe.agrodirecto.config.AplicacionConfig;
import pe.agrodirecto.pedidos.servicio.PedidoService;

/** RN-15, RN-20, RN-21, RN-22: reseñas verificadas, reputación y disputas con descargo. */
class ConfianzaTest extends PruebaIntegracion {

    private static final String RECLAMO_CALIDAD = """
            {"motivo":"CALIDAD","descripcion":"Llegaron papas golpeadas en 1 de los 2 sacos.",
             "evidenciaUrl":"https://fotos.example/saco-golpeado.jpg"}""";

    @Autowired
    PedidoService pedidoService;

    // ---------- Reseñas ----------

    @Test
    void soloSeCalificaUnaCompraCompletadaYUnaSolaVez() throws Exception {
        String comprador = token(COMPRADOR);
        Long id = entregado(comprador);
        calificar(comprador, id, 5).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-20"));

        postCon(comprador, "/api/pedidos/" + id + "/confirmar-recepcion", null).andExpect(status().isOk());
        nuevaPeticion();
        calificar(comprador, id, 5).andExpect(status().isCreated())
                .andExpect(jsonPath("$.autor").value("Restaurante Sabor Andino"));
        calificar(comprador, id, 4).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje", containsString("Ya calificaste")));
    }

    @Test
    void nadieCalificaPedidosAjenosNiConNotasInvalidas() throws Exception {
        String comprador = token(COMPRADOR);
        Long id = completado(comprador);
        calificar(token(COMPRADOR_NUEVO), id, 1).andExpect(status().isForbidden());
        calificar(comprador, id, 6).andExpect(status().isBadRequest());
    }

    @Test
    void laReputacionSeCalculaConHechos() throws Exception {
        Long andesVerde = idProductor("40000004");
        getCon(null, "/api/productores/" + andesVerde + "/reputacion")
                .andExpect(jsonPath("$.nivel").value("NUEVO"))
                .andExpect(jsonPath("$.promedio").doesNotExist())
                .andExpect(jsonPath("$.tasaCumplimiento").doesNotExist());

        String comprador = token(COMPRADOR);
        for (int nota : new int[] {5, 4, 5}) {
            Long id = completado(comprador);
            calificar(comprador, id, nota).andExpect(status().isCreated());
        }
        Long rechazado = pagado(comprador);
        postCon(token(PRODUCTOR), "/api/gestion/pedidos/" + rechazado + "/rechazar", null);
        nuevaPeticion();

        getCon(null, "/api/productores/" + andesVerde + "/reputacion")
                .andExpect(jsonPath("$.promedio").value(4.7))
                .andExpect(jsonPath("$.totalResenas").value(3))
                .andExpect(jsonPath("$.pedidosCompletados").value(3))
                .andExpect(jsonPath("$.pedidosRechazados").value(1))
                .andExpect(jsonPath("$.tasaCumplimiento").value(75))
                .andExpect(jsonPath("$.nivel").value("REGULAR"));
        getCon(null, "/api/productores/" + andesVerde + "/resenas").andExpect(jsonPath("$.length()").value(3));
        getCon(null, "/api/reputaciones?productorIds=" + andesVerde + "," + idProductor("40000011"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---------- Disputas ----------

    @Test
    void unReclamoCongelaElPagoAunqueVenzaElPlazoDeLiberacion() throws Exception {
        String comprador = token(COMPRADOR);
        Long id = entregado(comprador);
        reportar(comprador, id, RECLAMO_CALIDAD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.montoEnJuego").value(272.80));

        jdbc.update("UPDATE pedido SET entregado_en = ? WHERE id = ?", hace(72), id);
        assertThat(pedidoService.liberarPagosVencidos()).isZero();

        nuevaPeticion();
        getCon(comprador, "/api/pedidos/" + id)
                .andExpect(jsonPath("$.estado").value("EN_DISPUTA"))
                .andExpect(jsonPath("$.pago.estado").value("RETENIDO"));
    }

    @Test
    void elAgricultorSeEnteraPorSmsYPresentaSuDescargo() throws Exception {
        String comprador = token(COMPRADOR);
        Long disputa = disputaAbierta(comprador);
        String productor = token(PRODUCTOR);

        getCon(productor, "/api/gestion/mensajes?productorId=" + idProductor("40000004"))
                .andExpect(jsonPath("$[?(@.canal == 'SMS')].mensaje",
                        hasItem(allOf(containsString("reporto un problema"), containsString("pago sigue asegurado")))));

        postCon(comprador, "/api/disputas/" + disputa + "/descargo", "{\"descargo\":\"Yo\"}")
                .andExpect(status().isForbidden());
        postCon(productor, "/api/disputas/" + disputa + "/descargo", """
                {"descargo":"Los sacos salieron sanos; adjunto foto del despacho.",
                 "evidenciaUrl":"https://fotos.example/despacho.jpg"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descargo", containsString("salieron sanos")));
        getCon(comprador, "/api/disputas/" + disputa).andExpect(jsonPath("$.evidenciaDescargoUrl").exists());
    }

    @Test
    void losReclamosSobreLoRecibidoExigenFotoYPlazo() throws Exception {
        String comprador = token(COMPRADOR);
        Long id = entregado(comprador);
        String sinFoto = RECLAMO_CALIDAD.replaceAll(",\\s*\"evidenciaUrl\":\"[^\"]+\"", "");
        reportar(comprador, id, sinFoto)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-15"));

        jdbc.update("UPDATE pedido SET entregado_en = ? WHERE id = ?", hace(25), id);
        nuevaPeticion();
        reportar(comprador, id, RECLAMO_CALIDAD)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje", containsString("24 horas")));
    }

    @Test
    void noSeReclamaAntesDeLaEntrega() throws Exception {
        String comprador = token(COMPRADOR);
        reportar(comprador, pagado(comprador), "{\"motivo\":\"NO_ENTREGADO\",\"descripcion\":\"No llega\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-15"));
    }

    @Test
    void laAdministracionResuelveAFavorDelComprador() throws Exception {
        String comprador = token(COMPRADOR);
        Long disputa = disputaAbierta(comprador);
        Long pedido = jdbc.queryForObject("SELECT pedido_id FROM disputa WHERE id = ?", Long.class, disputa);

        resolver(token(PRODUCTOR), disputa, "COMPRADOR").andExpect(status().isForbidden());
        resolver(token(ADMIN), disputa, "COMPRADOR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("A_FAVOR_COMPRADOR"))
                .andExpect(jsonPath("$.resueltaPor").value("Administración AgroDirecto"));
        resolver(token(ADMIN), disputa, "PRODUCTOR").andExpect(status().isUnprocessableContent());

        nuevaPeticion();
        getCon(comprador, "/api/pedidos/" + pedido)
                .andExpect(jsonPath("$.estado").value("REEMBOLSADO"))
                .andExpect(jsonPath("$.pago.estado").value("DEVUELTO"));
        getCon(null, "/api/productores/" + idProductor("40000004") + "/reputacion")
                .andExpect(jsonPath("$.disputasPerdidas").value(1));
    }

    @Test
    void laAdministracionResuelveAFavorDelAgricultor() throws Exception {
        Long disputa = disputaAbierta(token(COMPRADOR));
        Long pedido = jdbc.queryForObject("SELECT pedido_id FROM disputa WHERE id = ?", Long.class, disputa);

        getCon(token(ADMIN), "/api/admin/disputas?estado=ABIERTA").andExpect(jsonPath("$.length()").value(1));
        resolver(token(ADMIN), disputa, "PRODUCTOR").andExpect(jsonPath("$.estado").value("A_FAVOR_PRODUCTOR"));

        nuevaPeticion();
        getCon(token(PRODUCTOR), "/api/pedidos/" + pedido)
                .andExpect(jsonPath("$.estado").value("COMPLETADO"))
                .andExpect(jsonPath("$.pago.estado").value("LIBERADO"));
    }

    // ---------- Apoyo ----------

    private Long pagado(String comprador) throws Exception {
        String cuerpo = postCon(comprador, "/api/pedidos", """
                {"items":[{"formatoId":1,"cantidad":2}],"modalidadEntrega":"ENTREGA_DEL_PRODUCTOR",
                 "regionDestino":"Lima","direccionEntrega":"Av. Javier Prado 2450, San Borja"}""")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long id = ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
        postCon(comprador, "/api/pedidos/" + id + "/pago", "{\"metodo\":\"YAPE\"}").andExpect(status().isOk());
        nuevaPeticion();
        return id;
    }

    private Long entregado(String comprador) throws Exception {
        Long id = pagado(comprador);
        String productor = token(PRODUCTOR);
        postCon(productor, "/api/gestion/pedidos/" + id + "/aceptar", null).andExpect(status().isOk());
        for (String estado : new String[] {"EN_PREPARACION", "LISTO_PARA_ENTREGA", "ENTREGADO"}) {
            postCon(productor, "/api/gestion/pedidos/" + id + "/estado", "{\"estado\":\"" + estado + "\"}")
                    .andExpect(status().isOk());
        }
        nuevaPeticion();
        return id;
    }

    private Long completado(String comprador) throws Exception {
        Long id = entregado(comprador);
        postCon(comprador, "/api/pedidos/" + id + "/confirmar-recepcion", null).andExpect(status().isOk());
        nuevaPeticion();
        return id;
    }

    private Long disputaAbierta(String comprador) throws Exception {
        String cuerpo = reportar(comprador, entregado(comprador), RECLAMO_CALIDAD).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        nuevaPeticion();
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }

    private ResultActions calificar(String token, Long pedido, int nota) throws Exception {
        return postCon(token, "/api/pedidos/" + pedido + "/resena",
                "{\"calificacion\":" + nota + ",\"comentario\":\"Buena papa\"}");
    }

    private ResultActions reportar(String token, Long pedido, String json) throws Exception {
        return postCon(token, "/api/pedidos/" + pedido + "/disputa", json);
    }

    private ResultActions resolver(String token, Long disputa, String aFavorDe) throws Exception {
        return postCon(token, "/api/admin/disputas/" + disputa + "/resolver",
                "{\"aFavorDe\":\"" + aFavorDe + "\",\"resolucion\":\"Revisadas las fotos de ambas partes.\"}");
    }

    private static LocalDateTime hace(int horas) {
        return LocalDateTime.now(AplicacionConfig.ZONA_PERU).minusHours(horas);
    }
}
