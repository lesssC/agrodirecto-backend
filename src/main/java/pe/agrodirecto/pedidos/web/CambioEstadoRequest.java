package pe.agrodirecto.pedidos.web;

import jakarta.validation.constraints.NotNull;
import pe.agrodirecto.pedidos.dominio.EstadoPedido;

/** Avance del pedido por el agricultor: EN_PREPARACION, LISTO_PARA_ENTREGA o ENTREGADO. */
public record CambioEstadoRequest(@NotNull EstadoPedido estado, String comentario) {
}
