package pe.agrodirecto.tiemporeal.config;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Canales a los que el frontend puede suscribirse.
 *
 * <ul>
 * <li>{@code /user/queue/pedidos}: pedidos propios del comprador (privado).</li>
 * <li>{@code /topic/productores/{id}/pedidos}: pedidos recibidos de un productor (solo quien lo gestiona).</li>
 * <li>{@code /topic/productos/{id}/stock}: stock de un producto (público).</li>
 * </ul>
 */
public final class Destinos {

    public static final String MIS_PEDIDOS = "/queue/pedidos";
    public static final String MIS_PEDIDOS_SUSCRIPCION = "/user" + MIS_PEDIDOS;

    private static final Pattern PEDIDOS_PRODUCTOR = Pattern.compile("^/topic/productores/(\\d+)/pedidos$");
    private static final Pattern STOCK_PRODUCTO = Pattern.compile("^/topic/productos/(\\d+)/stock$");

    private Destinos() {
    }

    public static String pedidosDelProductor(Long productorId) {
        return "/topic/productores/" + productorId + "/pedidos";
    }

    public static String stockDelProducto(Long productoId) {
        return "/topic/productos/" + productoId + "/stock";
    }

    public static Optional<Long> productorDe(String destino) {
        return idDe(PEDIDOS_PRODUCTOR, destino);
    }

    public static boolean esStockPublico(String destino) {
        return destino != null && STOCK_PRODUCTO.matcher(destino).matches();
    }

    private static Optional<Long> idDe(Pattern patron, String destino) {
        if (destino == null) {
            return Optional.empty();
        }
        Matcher m = patron.matcher(destino);
        return m.matches() ? Optional.of(Long.valueOf(m.group(1))) : Optional.empty();
    }
}
