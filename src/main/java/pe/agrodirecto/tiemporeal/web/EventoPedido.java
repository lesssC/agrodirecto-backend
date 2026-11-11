package pe.agrodirecto.tiemporeal.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import pe.agrodirecto.pedidos.dominio.CanalOperacion;
import pe.agrodirecto.pedidos.dominio.EstadoPedido;

/** Mensaje en tiempo real cuando un pedido cambia de estado. El frontend actualiza la pantalla sin recargar. */
public record EventoPedido(String tipo, Long pedidoId, String codigo, EstadoPedido anterior, EstadoPedido estado,
        CanalOperacion canal, Long productorId, String productor, String comprador, BigDecimal total,
        BigDecimal montoProductor, LocalDateTime fecha) {
}
