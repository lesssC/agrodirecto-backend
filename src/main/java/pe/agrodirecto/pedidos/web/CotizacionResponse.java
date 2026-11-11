package pe.agrodirecto.pedidos.web;

import java.time.LocalDate;
import java.util.List;

import pe.agrodirecto.pedidos.dominio.ModalidadEntrega;

/** Vista previa del pedido antes de confirmarlo: mismas validaciones y montos, sin reservar stock. */
public record CotizacionResponse(Long productorId, String productor, boolean emiteFactura,
        ModalidadEntrega modalidadEntrega, String regionDestino, int diasTransito, LocalDate fechaEntregaEstimada,
        List<LineaPedidoResponse> lineas, DesgloseResponse desglose) {
}
