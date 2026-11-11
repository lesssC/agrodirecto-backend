package pe.agrodirecto.pedidos.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import pe.agrodirecto.pedidos.dominio.CanalOperacion;
import pe.agrodirecto.pedidos.dominio.EstadoPedido;
import pe.agrodirecto.pedidos.dominio.ModalidadEntrega;

/**
 * Pedido visto por el comprador o por quien gestiona al productor. Los datos de contacto llegan en {@code null}
 * hasta que el agricultor acepta (RN-24). {@code acciones} indica qué puede hacer quien consulta.
 */
public record PedidoResponse(Long id, String codigo, EstadoPedido estado, LocalDateTime creadoEn,
        LocalDateTime actualizadoEn, LocalDate fechaEntregaEstimada, ModalidadEntrega modalidadEntrega,
        String regionDestino, String direccionEntrega, String nota, ProductorPedido productor,
        CompradorPedido comprador, List<LineaPedidoResponse> lineas, DesgloseResponse desglose,
        List<HistorialResponse> historial, List<String> acciones) {

    public record ProductorPedido(Long id, String nombreComercial, String region, boolean emiteFactura,
            String nombreTitular, String telefono) {
    }

    public record CompradorPedido(String nombre, String razonSocial, boolean verificado, String telefono) {
    }

    public record HistorialResponse(EstadoPedido estadoAnterior, EstadoPedido estadoNuevo, CanalOperacion canal,
            String usuario, String comentario, LocalDateTime fecha) {
    }
}
