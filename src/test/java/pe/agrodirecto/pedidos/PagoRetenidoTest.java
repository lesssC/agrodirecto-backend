package pe.agrodirecto.pedidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.jayway.jsonpath.JsonPath;

import pe.agrodirecto.PruebaIntegracion;
import pe.agrodirecto.pedidos.servicio.PedidoService;

/** RN-C1 y RN-C2: el comprador paga primero, el dinero queda retenido y se libera al agricultor tras la entrega. */
class PagoRetenidoTest extends PruebaIntegracion {

    private static final String PEDIDO_PAPA = """
            {"items":[{"formatoId":1,"cantidad":2}],"modalidadEntrega":"ENTREGA_DEL_PRODUCTOR",
             "regionDestino":"Lima","direccionEntrega":"Av. Javier Prado 2450, San Borja"}""";

    @Autowired
    PedidoService pedidoService;

    @Test
    void elAgricultorNoVeNiAtiendePedidosSinPagar() throws Exception {
        Long id = registrar();
        String productor = token(PRODUCTOR);
        getCon(productor, "/api/gestion/pedidos").andExpect(jsonPath("$.length()").value(0));
        postCon(productor, "/api/gestion/pedidos/" + id + "/aceptar", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-05"));
    }

    @Test
    void alPagarElDineroQuedaRetenidoYElPedidoLlegaAlAgricultor() throws Exception {
        String comprador = token(COMPRADOR);
        Long id = registrar();
        pagar(comprador, id, "PLIN")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.pago.estado").value("RETENIDO"))
                .andExpect(jsonPath("$.pago.metodo").value("PLIN"))
                .andExpect(jsonPath("$.pago.referencia", startsWith("SIM-")))
                .andExpect(jsonPath("$.pago.mensaje", containsString("hasta que se confirme la entrega")))
                .andExpect(jsonPath("$.historial[1].comentario").value("Pago retenido vía PLIN"));

        nuevaPeticion();
        getCon(token(PRODUCTOR), "/api/gestion/pedidos")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pago.estado").value("RETENIDO"));
    }

    @Test
    void noSePagaDosVecesNiSePagaUnPedidoAjeno() throws Exception {
        String comprador = token(COMPRADOR);
        Long id = registrar();
        pagar(token(COMPRADOR_NUEVO), id, "YAPE").andExpect(status().isForbidden());
        pagar(comprador, id, "YAPE").andExpect(status().isOk());
        pagar(comprador, id, "YAPE")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-C1"));
        postCon(comprador, "/api/pedidos/" + id + "/pago", "{\"metodo\":\"BITCOIN\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void alConfirmarLaRecepcionSeLiberaElPagoAlMedioDelAgricultor() throws Exception {
        String comprador = token(COMPRADOR);
        Long id = registrarYEntregar(comprador);

        postCon(comprador, "/api/pedidos/" + id + "/confirmar-recepcion", null)
                .andExpect(jsonPath("$.estado").value("COMPLETADO"))
                .andExpect(jsonPath("$.pago.estado").value("LIBERADO"))
                .andExpect(jsonPath("$.pago.destino").value("YAPE ••••••004"))
                .andExpect(jsonPath("$.pago.mensaje").value(
                        "Se transfirieron S/ 261.00 al agricultor (YAPE ••••••004)."));

        nuevaPeticion();
        assertThat(jdbc.queryForObject("SELECT comision FROM pago WHERE pedido_id = ?", java.math.BigDecimal.class, id))
                .isEqualByComparingTo("11.80");
    }

    @Test
    void siElAgricultorRechazaSeDevuelveElDinero() throws Exception {
        String comprador = token(COMPRADOR);
        Long id = registrar();
        pagar(comprador, id, "YAPE");
        nuevaPeticion();
        postCon(token(PRODUCTOR), "/api/gestion/pedidos/" + id + "/rechazar", "{\"motivo\":\"Sin stock real\"}")
                .andExpect(jsonPath("$.pago.estado").value("DEVUELTO"));
    }

    @Test
    void cancelarAntesDePagarNoGeneraCobro() throws Exception {
        String comprador = token(COMPRADOR);
        Long id = registrar();
        postCon(comprador, "/api/pedidos/" + id + "/cancelar", null)
                .andExpect(jsonPath("$.estado").value("CANCELADO"))
                .andExpect(jsonPath("$.pago.estado").value("ANULADO"));
        assertThat(stockSaco()).isEqualTo(18);
    }

    @Test
    void sinReclamoEnCuarentaYOchoHorasElPagoSeLiberaSolo() throws Exception {
        Long id = registrarYEntregar(token(COMPRADOR));
        jdbc.update("UPDATE pedido SET entregado_en = ? WHERE id = ?", haceHoras(49), id);

        assertThat(pedidoService.liberarPagosVencidos()).isEqualTo(1);

        nuevaPeticion();
        getCon(token(COMPRADOR), "/api/pedidos/" + id)
                .andExpect(jsonPath("$.estado").value("COMPLETADO"))
                .andExpect(jsonPath("$.pago.estado").value("LIBERADO"))
                .andExpect(jsonPath("$.historial[6].canal").value("SISTEMA"))
                .andExpect(jsonPath("$.historial[6].comentario", containsString("48 horas")));
    }

    @Test
    void losPedidosQueNuncaSePaganLiberanSuStock() throws Exception {
        Long id = registrar();
        assertThat(stockSaco()).isEqualTo(16);
        jdbc.update("UPDATE pedido SET creado_en = ? WHERE id = ?", haceHoras(1).minusMinutes(1), id);

        assertThat(pedidoService.expirarPedidosImpagos()).isEqualTo(1);

        assertThat(stockSaco()).isEqualTo(18);
        getCon(token(COMPRADOR), "/api/pedidos/" + id)
                .andExpect(jsonPath("$.estado").value("CANCELADO"))
                .andExpect(jsonPath("$.pago.estado").value("ANULADO"));
    }

    @Test
    void unPedidoPagadoSinRespuestaDelProductorSeCancelaYDevuelveElDinero() throws Exception {
        String comprador = token(COMPRADOR);
        Long id = registrar();
        pagar(comprador, id, "YAPE").andExpect(status().isOk());
        nuevaPeticion();
        jdbc.update("UPDATE pedido SET actualizado_en = ? WHERE id = ?", haceHoras(25), id);
        entityManager.clear();
        assertThat(jdbc.queryForObject("SELECT actualizado_en FROM pedido WHERE id = ?", LocalDateTime.class, id))
                .isBefore(LocalDateTime.now(pe.agrodirecto.config.AplicacionConfig.ZONA_PERU).minusHours(24));

        assertThat(pedidoService.expirarPedidosSinRespuesta()).isEqualTo(1);

        assertThat(stockSaco()).isEqualTo(18);
        getCon(comprador, "/api/pedidos/" + id)
                .andExpect(jsonPath("$.estado").value("CANCELADO"))
                .andExpect(jsonPath("$.pago.estado").value("DEVUELTO"))
                .andExpect(jsonPath("$.historial[2].canal").value("SISTEMA"))
                .andExpect(jsonPath("$.historial[2].comentario")
                        .value("Cancelado por falta de respuesta del productor"));
    }

    // ---------- Apoyo ----------

    private Long registrar() throws Exception {
        String cuerpo = postCon(token(COMPRADOR), "/api/pedidos", PEDIDO_PAPA)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        nuevaPeticion();
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }

    private Long registrarYEntregar(String comprador) throws Exception {
        Long id = registrar();
        pagar(comprador, id, "YAPE").andExpect(status().isOk());
        String productor = token(PRODUCTOR);
        postCon(productor, "/api/gestion/pedidos/" + id + "/aceptar", null).andExpect(status().isOk());
        for (String estado : new String[] {"EN_PREPARACION", "LISTO_PARA_ENTREGA", "ENTREGADO"}) {
            postCon(productor, "/api/gestion/pedidos/" + id + "/estado", "{\"estado\":\"" + estado + "\"}")
                    .andExpect(status().isOk());
        }
        nuevaPeticion();
        return id;
    }

    private org.springframework.test.web.servlet.ResultActions pagar(String token, Long id, String metodo)
            throws Exception {
        return postCon(token, "/api/pedidos/" + id + "/pago", "{\"metodo\":\"" + metodo + "\"}");
    }

    /** La aplicación guarda la hora de Lima; las fechas simuladas deben usar la misma zona. */
    private static java.time.LocalDateTime haceHoras(int horas) {
        return java.time.LocalDateTime.now(pe.agrodirecto.config.AplicacionConfig.ZONA_PERU).minusHours(horas);
    }

    private int stockSaco() {
        nuevaPeticion();
        return jdbc.queryForObject("SELECT stock FROM formato_venta WHERE id = 1", Integer.class);
    }
}
