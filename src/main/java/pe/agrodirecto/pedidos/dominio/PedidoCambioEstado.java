package pe.agrodirecto.pedidos.dominio;

/**
 * Evento que el módulo de pedidos publica en cada cambio de estado. Otros módulos (notificaciones, tiempo real)
 * lo escuchan sin que {@code pedidos} dependa de ellos.
 *
 * @param anterior {@code null} cuando el pedido se acaba de registrar
 */
public record PedidoCambioEstado(Long pedidoId, String codigo, EstadoPedido anterior, EstadoPedido nuevo,
        CanalOperacion canal) {
}
