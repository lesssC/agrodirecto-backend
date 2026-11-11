package pe.agrodirecto.pedidos.servicio;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import pe.agrodirecto.pagos.dominio.Pago;
import pe.agrodirecto.pagos.web.PagoResponse;
import pe.agrodirecto.pedidos.dominio.DetallePedido;
import pe.agrodirecto.pedidos.dominio.EstadoPedido;
import pe.agrodirecto.pedidos.dominio.Pedido;
import pe.agrodirecto.pedidos.web.DesgloseResponse;
import pe.agrodirecto.pedidos.web.LineaPedidoResponse;
import pe.agrodirecto.pedidos.web.PedidoResponse;
import pe.agrodirecto.productores.dominio.Productor;
import pe.agrodirecto.usuarios.dominio.Usuario;

/** Arma la respuesta según quién mira: el comprador o quien gestiona al productor. */
@Component
public class PedidoMapper {

    public enum Vista {
        COMPRADOR,
        GESTOR
    }

    public PedidoResponse aRespuesta(Pedido p, Pago pago, Vista vista) {
        boolean contacto = p.getEstado().revelaContacto();
        Productor productor = p.getProductor();
        Usuario comprador = p.getComprador();
        return new PedidoResponse(p.getId(), p.getCodigo(), p.getEstado(), p.getCreadoEn(), p.getActualizadoEn(),
                p.getFechaEntregaEstimada(), p.getModalidadEntrega(), p.getRegionDestino(),
                // El comprador siempre ve su propia dirección; el agricultor, solo tras aceptar (RN-24).
                vista == Vista.COMPRADOR || contacto ? p.getDireccionEntrega() : null, p.getNota(),
                new PedidoResponse.ProductorPedido(productor.getId(), productor.getNombreComercial(),
                        productor.getRegion(), productor.emiteFactura(),
                        contacto ? productor.getNombreTitular() : null, contacto ? productor.getTelefono() : null),
                new PedidoResponse.CompradorPedido(comprador.getNombre(), comprador.getRazonSocial(),
                        comprador.isVerificado(), contacto ? comprador.getTelefono() : null),
                p.getDetalles().stream().map(d -> linea(d)).toList(),
                DesgloseResponse.de(p.getSubtotal(), p.getDescuento(), p.getCostoFlete(), p.getComision(),
                        p.getTotal(), p.getMontoProductor(), productor.getNombreComercial()),
                p.getHistorial().stream().map(h -> new PedidoResponse.HistorialResponse(h.getEstadoAnterior(),
                        h.getEstadoNuevo(), h.getCanal(), h.getUsuario() == null ? null : h.getUsuario().getNombre(),
                        h.getComentario(), h.getCreadoEn())).toList(),
                pago == null ? null : PagoResponse.de(pago),
                acciones(p.getEstado(), vista));
    }

    static LineaPedidoResponse linea(CalculadoraPedido.LineaCalculada l) {
        return new LineaPedidoResponse(l.formato().getId(), l.formato().getProducto().getId(),
                l.formato().getProducto().getNombre(), l.formato().getNombre(), l.cantidad(), l.precioUnitario(),
                l.porcentajeDescuento(), l.neto());
    }

    static LineaPedidoResponse linea(DetallePedido d) {
        return new LineaPedidoResponse(d.getFormato().getId(), d.getFormato().getProducto().getId(),
                d.getFormato().getProducto().getNombre(), d.getFormato().getNombre(), d.getCantidad(),
                d.getPrecioUnitario(), d.getPorcentajeDescuento(), d.getSubtotal());
    }

    /** Lo que el frontend puede ofrecer como botones en cada estado. */
    private static List<String> acciones(EstadoPedido estado, Vista vista) {
        List<String> acciones = new ArrayList<>();
        if (vista == Vista.COMPRADOR) {
            if (estado == EstadoPedido.PENDIENTE_PAGO) {
                acciones.add("PAGAR");
            }
            if (estado == EstadoPedido.PENDIENTE_PAGO || estado == EstadoPedido.PENDIENTE) {
                acciones.add("CANCELAR");
            }
            if (estado == EstadoPedido.ENTREGADO) {
                acciones.add("CONFIRMAR_RECEPCION");
            }
            return acciones;
        }
        switch (estado) {
            case PENDIENTE -> acciones.addAll(List.of("ACEPTAR", "RECHAZAR"));
            case ACEPTADO -> acciones.add("EN_PREPARACION");
            case EN_PREPARACION -> acciones.add("LISTO_PARA_ENTREGA");
            case LISTO_PARA_ENTREGA -> acciones.add("ENTREGADO");
            default -> {
            }
        }
        return acciones;
    }
}
