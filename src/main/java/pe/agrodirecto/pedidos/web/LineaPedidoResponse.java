package pe.agrodirecto.pedidos.web;

import java.math.BigDecimal;

public record LineaPedidoResponse(Long formatoId, Long productoId, String producto, String formato, int cantidad,
        BigDecimal precioUnitario, BigDecimal porcentajeDescuento, BigDecimal subtotal) {
}
