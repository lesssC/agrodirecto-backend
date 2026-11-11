package pe.agrodirecto.pedidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import pe.agrodirecto.PruebaIntegracion;
import pe.agrodirecto.config.AplicacionConfig;

class PedidoControllerTest extends PruebaIntegracion {

    // ---------- Cotización y transparencia ----------

    @Test
    void cotizaConElDesgloseTransparente() throws Exception {
        postCon(token(COMPRADOR), "/api/pedidos/cotizacion",
                pedido("Lima", "ENTREGA_DEL_PRODUCTOR", item(papa("Saco de 50 kg"), 2), item(papa("Arroba de 12.5 kg"), 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productor").value("AndesVerde"))
                .andExpect(jsonPath("$.diasTransito").value(1))
                .andExpect(jsonPath("$.fechaEntregaEstimada").value(LocalDate.now(AplicacionConfig.ZONA_PERU).plusDays(2).toString()))
                .andExpect(jsonPath("$.desglose.subtotal").value(268.00))
                .andExpect(jsonPath("$.desglose.costoFlete").value(25.00))
                .andExpect(jsonPath("$.desglose.comision").value(13.40))
                .andExpect(jsonPath("$.desglose.total").value(306.40))
                .andExpect(jsonPath("$.desglose.montoProductor").value(293.00))
                .andExpect(jsonPath("$.desglose.mensaje", containsString("S/ 293.00 van íntegros a AndesVerde")));
    }

    @Test
    void aplicaElDescuentoPorVolumenQueDefinioElAgricultor() throws Exception {
        postCon(token(COMPRADOR), "/api/pedidos/cotizacion", pedido("Lima", "ENTREGA_DEL_PRODUCTOR",
                item(papa("Saco de 50 kg"), 10)))
                .andExpect(jsonPath("$.lineas[0].porcentajeDescuento").value(5.00))
                .andExpect(jsonPath("$.desglose.subtotal").value(1180.00))
                .andExpect(jsonPath("$.desglose.descuento").value(59.00))
                .andExpect(jsonPath("$.desglose.comision").value(56.05))
                .andExpect(jsonPath("$.desglose.total").value(1202.05));
    }

    @Test
    void elPrecioLoPoneElServidorNoElComprador() throws Exception {
        String json = pedido("Lima", "ENTREGA_DEL_PRODUCTOR", item(papa("Saco de 50 kg"), 2))
                .replace("\"cantidad\":2", "\"cantidad\":2,\"precioUnitario\":1");
        postCon(token(COMPRADOR), "/api/pedidos/cotizacion", json)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas[0].precioUnitario").value(118.00));
    }

    // ---------- Reglas al armar el pedido ----------

    @Test
    void unPedidoAgrupaUnSoloProductor() throws Exception {
        postCon(token(COMPRADOR), "/api/pedidos", pedido("Lima", "ENTREGA_DEL_PRODUCTOR",
                item(papa("Saco de 50 kg"), 2), item(formato("Tomate italiano", "Caja de 20 kg"), 3)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-01"));
    }

    @Test
    void soloSeEntregaEnRegionesQueElProductorCubre() throws Exception {
        String comprador = token(COMPRADOR);
        postCon(comprador, "/api/pedidos", pedido("Cusco", "ENTREGA_DEL_PRODUCTOR", item(papa("Saco de 50 kg"), 2)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-09"));
        // Con recojo en chacra no hay restricción de zona ni flete.
        postCon(comprador, "/api/pedidos/cotizacion", pedido("Cusco", "RECOJO_EN_CHACRA", item(papa("Saco de 50 kg"), 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desglose.costoFlete").value(0))
                .andExpect(jsonPath("$.desglose.montoProductor").value(236.00));
    }

    @Test
    void noSeVendeLoQueLlegariaMalogrado() throws Exception {
        String comprador = token(COMPRADOR);
        // Tomate cherry dura 3 días; el viaje de Ica a Cusco toma 4.
        postCon(comprador, "/api/pedidos", pedido("Cusco", "AGENCIA_DE_CARGA",
                item(formato("Tomate cherry", "Caja de 10 kg"), 10)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-10"));
        // El tomate italiano dura 10 días: sí llega bien.
        postCon(comprador, "/api/pedidos/cotizacion", pedido("Cusco", "AGENCIA_DE_CARGA",
                item(formato("Tomate italiano", "Caja de 20 kg"), 8)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desglose.costoFlete").value(90.00));
    }

    @Test
    void respetaElPedidoMinimoDeLaZona() throws Exception {
        postCon(token(COMPRADOR), "/api/pedidos", pedido("Lima", "ENTREGA_DEL_PRODUCTOR",
                item(papa("Bolsa de 5 kg"), 1)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-11"))
                .andExpect(jsonPath("$.mensaje", containsString("S/ 150.00")));
    }

    @Test
    void noSePuedePedirMasDelStockDisponible() throws Exception {
        postCon(token(COMPRADOR), "/api/pedidos", pedido("Lima", "ENTREGA_DEL_PRODUCTOR",
                item(papa("Saco de 50 kg"), 19)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-03"));
    }

    @Test
    void unaCuentaNuevaTieneLimitesDeMontoYPedidosAbiertos() throws Exception {
        String nuevo = token(COMPRADOR_NUEVO);
        postCon(nuevo, "/api/pedidos", pedido("Lima", "ENTREGA_DEL_PRODUCTOR", item(papa("Saco de 50 kg"), 13)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-18"))
                .andExpect(jsonPath("$.mensaje", containsString("S/ 1500")));

        crear(nuevo, pedido("Lima", "ENTREGA_DEL_PRODUCTOR", item(papa("Saco de 50 kg"), 2)));
        crear(nuevo, pedido("Lima", "ENTREGA_DEL_PRODUCTOR", item(papa("Saco de 50 kg"), 2)));
        postCon(nuevo, "/api/pedidos", pedido("Lima", "ENTREGA_DEL_PRODUCTOR", item(papa("Saco de 50 kg"), 2)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje", containsString("2 pedidos en curso")));
    }

    @Test
    void soloLosCompradoresHacenPedidos() throws Exception {
        postCon(token(PRODUCTOR), "/api/pedidos", pedido("Lima", "ENTREGA_DEL_PRODUCTOR", item(papa("Saco de 50 kg"), 2)))
                .andExpect(status().isForbidden());
    }

    // ---------- Registro y ciclo de vida ----------

    @Test
    void registraElPedidoYReservaElStock() throws Exception {
        Long saco = papa("Saco de 50 kg");
        postCon(token(COMPRADOR), "/api/pedidos", pedido("Lima", "ENTREGA_DEL_PRODUCTOR", item(saco, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo", matchesPattern("AG-25\\d\\d")))
                .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"))
                .andExpect(jsonPath("$.acciones[0]").value("PAGAR"))
                .andExpect(jsonPath("$.acciones[1]").value("CANCELAR"))
                .andExpect(jsonPath("$.pago.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.historial.length()").value(1));
        assertThat(stock(saco)).isEqualTo(16);
    }

    @Test
    void elCicloCompletoQuedaEnElHistorial() throws Exception {
        String comprador = token(COMPRADOR);
        String productor = token(PRODUCTOR);
        Long id = crear(comprador, pedido("Lima", "ENTREGA_DEL_PRODUCTOR", item(papa("Saco de 50 kg"), 2)));

        postCon(productor, "/api/gestion/pedidos/" + id + "/aceptar", null)
                .andExpect(jsonPath("$.estado").value("ACEPTADO"))
                .andExpect(jsonPath("$.acciones[0]").value("EN_PREPARACION"));
        avanzar(productor, id, "EN_PREPARACION").andExpect(status().isOk());
        avanzar(productor, id, "LISTO_PARA_ENTREGA").andExpect(status().isOk());
        avanzar(productor, id, "ENTREGADO").andExpect(status().isOk());

        postCon(comprador, "/api/pedidos/" + id + "/confirmar-recepcion", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADO"))
                .andExpect(jsonPath("$.historial.length()").value(7))
                .andExpect(jsonPath("$.historial[2].usuario").value("Juan Huamán Rojas"))
                .andExpect(jsonPath("$.historial[6].estadoNuevo").value("COMPLETADO"));
    }

    @Test
    void noSePuedeSaltarEstados() throws Exception {
        Long id = crear(token(COMPRADOR), pedido("Lima", "ENTREGA_DEL_PRODUCTOR", item(papa("Saco de 50 kg"), 2)));
        String productor = token(PRODUCTOR);
        avanzar(productor, id, "ENTREGADO")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-05"));
        avanzar(productor, id, "ACEPTADO").andExpect(status().isUnprocessableContent());
    }

    @Test
    void losDatosDeContactoSeRevelanAlAceptar() throws Exception {
        String comprador = token(COMPRADOR);
        String productor = token(PRODUCTOR);
        Long id = crear(comprador, pedido("Lima", "ENTREGA_DEL_PRODUCTOR", item(papa("Saco de 50 kg"), 2)));

        getCon(comprador, "/api/pedidos/" + id)
                .andExpect(jsonPath("$.productor.telefono").doesNotExist())
                .andExpect(jsonPath("$.direccionEntrega").value("Av. Javier Prado 2450, San Borja"));
        getCon(productor, "/api/pedidos/" + id)
                .andExpect(jsonPath("$.direccionEntrega").doesNotExist())
                .andExpect(jsonPath("$.comprador.telefono").doesNotExist())
                .andExpect(jsonPath("$.comprador.razonSocial").value("Sabor Andino S.A.C."))
                .andExpect(jsonPath("$.acciones[0]").value("ACEPTAR"));

        postCon(productor, "/api/gestion/pedidos/" + id + "/aceptar", null).andExpect(status().isOk());

        getCon(comprador, "/api/pedidos/" + id).andExpect(jsonPath("$.productor.telefono").value("900000004"));
        getCon(productor, "/api/pedidos/" + id)
                .andExpect(jsonPath("$.direccionEntrega").value("Av. Javier Prado 2450, San Borja"))
                .andExpect(jsonPath("$.comprador.telefono").value("900000002"));
    }

    @Test
    void rechazarOCancelarDevuelveElStock() throws Exception {
        String comprador = token(COMPRADOR);
        Long saco = papa("Saco de 50 kg");
        Long rechazado = crear(comprador, pedido("Lima", "ENTREGA_DEL_PRODUCTOR", item(saco, 3)));
        Long cancelado = crear(comprador, pedido("Lima", "ENTREGA_DEL_PRODUCTOR", item(saco, 2)));
        assertThat(stock(saco)).isEqualTo(13);

        postCon(token(PRODUCTOR), "/api/gestion/pedidos/" + rechazado + "/rechazar", "{\"motivo\":\"Helada en la zona\"}")
                .andExpect(jsonPath("$.estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.historial[2].comentario").value("Helada en la zona"));
        postCon(comprador, "/api/pedidos/" + cancelado + "/cancelar", null)
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
        assertThat(stock(saco)).isEqualTo(18);
    }

    @Test
    void elCompradorNoCancelaUnPedidoYaAceptado() throws Exception {
        String comprador = token(COMPRADOR);
        Long id = crear(comprador, pedido("Lima", "ENTREGA_DEL_PRODUCTOR", item(papa("Saco de 50 kg"), 2)));
        postCon(token(PRODUCTOR), "/api/gestion/pedidos/" + id + "/aceptar", null).andExpect(status().isOk());
        postCon(comprador, "/api/pedidos/" + id + "/cancelar", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.regla").value("RN-05"));
    }

    // ---------- Permisos ----------

    @Test
    void cadaQuienVeSoloSusPedidos() throws Exception {
        Long id = crear(token(COMPRADOR), pedido("Lima", "ENTREGA_DEL_PRODUCTOR", item(papa("Saco de 50 kg"), 2)));
        getCon(token(COMPRADOR_NUEVO), "/api/pedidos/" + id).andExpect(status().isForbidden());
        // La promotora no gestiona a AndesVerde.
        postCon(token(AGENTE), "/api/gestion/pedidos/" + id + "/aceptar", null).andExpect(status().isForbidden());
        getCon(token(PRODUCTOR), "/api/gestion/pedidos").andExpect(jsonPath("$.length()").value(1));
        getCon(token(AGENTE), "/api/gestion/pedidos").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void elAgenteAtiendeLosPedidosDeSusAgricultores() throws Exception {
        Long id = crear(token(COMPRADOR), pedido("Lima", "AGENCIA_DE_CARGA",
                item(formato("Tomate italiano", "Caja de 20 kg"), 3)));
        postCon(token(AGENTE), "/api/gestion/pedidos/" + id + "/aceptar", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historial[2].usuario").value("Rosa Quispe Mamani"));
        getCon(token(COMPRADOR), "/api/pedidos?estado=ACEPTADO").andExpect(jsonPath("$.length()").value(1));
    }

    // ---------- Apoyo ----------

    /** Registra el pedido y lo paga, que es el flujo normal antes de que el agricultor lo atienda. */
    private Long crear(String token, String json) throws Exception {
        String cuerpo = postCon(token, "/api/pedidos", json).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
        postCon(token, "/api/pedidos/" + id + "/pago", "{\"metodo\":\"YAPE\"}").andExpect(status().isOk());
        nuevaPeticion();
        return id;
    }

    private ResultActions avanzar(String token, Long id, String estado) throws Exception {
        return postCon(token, "/api/gestion/pedidos/" + id + "/estado", "{\"estado\":\"" + estado + "\"}");
    }

    private Long papa(String formato) {
        return formato("Papa amarilla seleccionada", formato);
    }

    private Long formato(String producto, String formato) {
        return jdbc.queryForObject("""
                SELECT f.id FROM formato_venta f JOIN producto p ON p.id = f.producto_id
                WHERE p.nombre = ? AND f.nombre = ?""", Long.class, producto, formato);
    }

    private int stock(Long formatoId) {
        nuevaPeticion();
        return jdbc.queryForObject("SELECT stock FROM formato_venta WHERE id = ?", Integer.class, formatoId);
    }

    private static String item(Long formatoId, int cantidad) {
        return "{\"formatoId\":" + formatoId + ",\"cantidad\":" + cantidad + "}";
    }

    private static String pedido(String region, String modalidad, String... items) {
        return """
                {"items":[%s],"modalidadEntrega":"%s","regionDestino":"%s",
                 "direccionEntrega":"Av. Javier Prado 2450, San Borja","nota":"Recepción de 9:00 a 13:00"}"""
                .formatted(String.join(",", items), modalidad, region);
    }
}
