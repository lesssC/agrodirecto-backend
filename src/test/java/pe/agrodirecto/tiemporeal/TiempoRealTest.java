package pe.agrodirecto.tiemporeal;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.jayway.jsonpath.JsonPath;

import pe.agrodirecto.usuarios.seguridad.Totp;
import pe.agrodirecto.usuarios.servicio.DobleFactorDemo;

/**
 * Prueba de punta a punta con un servidor real: clientes STOMP conectados mientras otro usuario opera por REST.
 * Usa su propia base en memoria porque aquí los cambios sí se confirman (commit).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties =
        "spring.datasource.url=jdbc:h2:mem:agrodirecto_ws;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class TiempoRealTest {

    private static final String PEDIDO = """
            {"items":[{"formatoId":1,"cantidad":2}],"modalidadEntrega":"ENTREGA_DEL_PRODUCTOR",
             "regionDestino":"Lima","direccionEntrega":"Av. Javier Prado 2450, San Borja"}""";

    @LocalServerPort
    int puerto;

    RestClient api;
    WebSocketStompClient stomp;
    /** Motivos que el servidor envía en los frames ERROR de STOMP. */
    BlockingQueue<String> errores;

    @BeforeEach
    void preparar() {
        api = RestClient.create("http://localhost:" + puerto);
        stomp = new WebSocketStompClient(new StandardWebSocketClient());
        stomp.setMessageConverter(new JacksonJsonMessageConverter());
        errores = new LinkedBlockingQueue<>();
    }

    @AfterEach
    void cerrar() {
        stomp.stop();
    }

    @Test
    void compradorYAgricultorVenLosCambiosAlInstante() throws Exception {
        String comprador = token("compras@saborandino.pe");
        String productor = token("andesverde@agrodirecto.pe");
        BlockingQueue<Map<String, Object>> delComprador = escuchar(conectar(comprador), "/user/queue/pedidos");
        BlockingQueue<Map<String, Object>> delAgricultor = escuchar(conectar(productor), "/topic/productores/1/pedidos");

        Long id = crearPedido(comprador);
        assertThat(siguiente(delComprador)).containsEntry("estado", "PENDIENTE_PAGO");
        assertThat(delAgricultor.poll(1, TimeUnit.SECONDS)).as("el agricultor no ve pedidos sin pagar").isNull();

        post(comprador, "/api/pedidos/" + id + "/pago", "{\"metodo\":\"YAPE\"}");
        Map<String, Object> nuevo = siguiente(delAgricultor);
        assertThat(nuevo).containsEntry("estado", "PENDIENTE").containsEntry("montoProductor", 261.0);
        assertThat(siguiente(delComprador)).containsEntry("estado", "PENDIENTE");

        post(productor, "/api/gestion/pedidos/" + id + "/aceptar", null);
        assertThat(siguiente(delComprador)).containsEntry("estado", "ACEPTADO").containsEntry("anterior", "PENDIENTE");
    }

    @Test
    void elStockDelCatalogoSeActualizaParaCualquierVisitante() throws Exception {
        BlockingQueue<Map<String, Object>> stock = escuchar(conectar(null), "/topic/productos/1/stock");

        Long id = crearPedido(token("compras@saborandino.pe"));
        int reservado = stockDelSaco(siguiente(stock));

        post(token("compras@saborandino.pe"), "/api/pedidos/" + id + "/cancelar", null);
        assertThat(stockDelSaco(siguiente(stock))).isEqualTo(reservado + 2);
    }

    @Test
    void nadieEscuchaLosPedidosDeUnProductorQueNoGestiona() throws Exception {
        StompSession comprador = conectar(token("compras@saborandino.pe"));
        comprador.subscribe("/topic/productores/1/pedidos", new Recolector(new LinkedBlockingQueue<>()));
        assertThat(seDesconecta(comprador)).isTrue();
        assertThat(errores.poll(5, TimeUnit.SECONDS)).contains("No gestionas al productor 1");

        // La promotora tampoco: AndesVerde tiene cuenta propia y no la gestiona ella.
        StompSession agente = conectar(token("promotora@agrodirecto.pe"));
        agente.subscribe("/topic/productores/1/pedidos", new Recolector(new LinkedBlockingQueue<>()));
        assertThat(errores.poll(5, TimeUnit.SECONDS)).contains("No gestionas al productor 1");

        StompSession anonimo = conectar(null);
        anonimo.subscribe("/user/queue/pedidos", new Recolector(new LinkedBlockingQueue<>()));
        Thread.sleep(300); // el broker procesa la suscripción de forma asíncrona
        assertThat(seDesconecta(anonimo)).isTrue();
        assertThat(errores.poll(5, TimeUnit.SECONDS)).contains("Inicia sesión");
    }

    @Test
    void rechazaUnTokenFalso() throws Exception {
        try {
            conectar("token.falso.123");
        } catch (ExecutionException | TimeoutException e) {
            // el cliente puede no completar la conexión: lo importante es el motivo que envió el servidor
        }
        assertThat(errores.poll(5, TimeUnit.SECONDS)).contains("Token inválido");
    }

    // ---------- Apoyo ----------

    private StompSession conectar(String token) throws Exception {
        StompHeaders cabeceras = new StompHeaders();
        if (token != null) {
            cabeceras.add("Authorization", "Bearer " + token);
        }
        return stomp.connectAsync("ws://localhost:" + puerto + "/ws", new WebSocketHttpHeaders(), cabeceras,
                new StompSessionHandlerAdapter() {
                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        errores.add(String.valueOf(headers.getFirst("message")));
                    }
                }).get(5, TimeUnit.SECONDS);
    }

    private BlockingQueue<Map<String, Object>> escuchar(StompSession sesion, String destino) throws Exception {
        BlockingQueue<Map<String, Object>> cola = new LinkedBlockingQueue<>();
        sesion.subscribe(destino, new Recolector(cola));
        Thread.sleep(300); // la suscripción se registra de forma asíncrona en el broker
        return cola;
    }

    private static Map<String, Object> siguiente(BlockingQueue<Map<String, Object>> cola) throws InterruptedException {
        Map<String, Object> mensaje = cola.poll(5, TimeUnit.SECONDS);
        assertThat(mensaje).as("se esperaba un mensaje en tiempo real").isNotNull();
        return mensaje;
    }

    private static boolean seDesconecta(StompSession sesion) throws InterruptedException {
        for (int i = 0; i < 50 && sesion.isConnected(); i++) {
            Thread.sleep(100);
        }
        return !sesion.isConnected();
    }

    @SuppressWarnings("unchecked")
    private static int stockDelSaco(Map<String, Object> evento) {
        return ((java.util.List<Map<String, Object>>) evento.get("formatos")).stream()
                .filter(f -> "Saco de 50 kg".equals(f.get("nombre")))
                .map(f -> ((Number) f.get("stock")).intValue()).findFirst().orElseThrow();
    }

    private String token(String email) {
        String cuerpo = api.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .body("{\"email\":\"" + email + "\",\"password\":\"demo1234\"}").retrieve().body(String.class);
        if (Boolean.TRUE.equals(JsonPath.read(cuerpo, "$.requiereSegundoFactor"))) {
            String codigo = Totp.codigoActual(DobleFactorDemo.SECRETOS.get(email), Instant.now());
            cuerpo = api.post().uri("/api/auth/2fa/verificar").contentType(MediaType.APPLICATION_JSON)
                    .body("{\"tokenTemporal\":\"" + JsonPath.read(cuerpo, "$.tokenTemporal") + "\",\"codigo\":\""
                            + codigo + "\"}")
                    .retrieve().body(String.class);
        }
        return JsonPath.read(cuerpo, "$.token");
    }

    private Long crearPedido(String token) {
        return ((Number) JsonPath.read(post(token, "/api/pedidos", PEDIDO), "$.id")).longValue();
    }

    private String post(String token, String uri, String json) {
        return api.post().uri(uri).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .body(json == null ? "" : json).retrieve().body(String.class);
    }

    private record Recolector(BlockingQueue<Map<String, Object>> cola) implements StompFrameHandler {

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return Map.class;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void handleFrame(StompHeaders headers, Object payload) {
            cola.add((Map<String, Object>) payload);
        }
    }
}
