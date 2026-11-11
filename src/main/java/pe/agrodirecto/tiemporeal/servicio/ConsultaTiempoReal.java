package pe.agrodirecto.tiemporeal.servicio;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.catalogo.dominio.FormatoVenta;
import pe.agrodirecto.catalogo.dominio.Producto;
import pe.agrodirecto.pedidos.dominio.EstadoPedido;
import pe.agrodirecto.pedidos.dominio.Pedido;
import pe.agrodirecto.pedidos.dominio.PedidoCambioEstado;
import pe.agrodirecto.pedidos.repositorio.PedidoRepository;
import pe.agrodirecto.tiemporeal.web.EventoPedido;
import pe.agrodirecto.tiemporeal.web.EventoStock;

/** Arma los mensajes leyendo el pedido ya confirmado en la base (corre después del commit). */
@Service
@RequiredArgsConstructor
public class ConsultaTiempoReal {

    private final PedidoRepository pedidos;

    public record Envio(Long compradorId, Long productorId, boolean visibleParaProductor, EventoPedido pedido,
            List<EventoStock> stock) {
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<Envio> preparar(PedidoCambioEstado evento) {
        return pedidos.findById(evento.pedidoId()).map(p -> new Envio(p.getComprador().getId(),
                p.getProductor().getId(), visibleParaProductor(evento), eventoPedido(p, evento),
                cambiaStock(evento.nuevo()) ? stock(p) : List.of()));
    }

    /** El agricultor no se entera de pedidos sin pagar, igual que en la API REST. */
    private static boolean visibleParaProductor(PedidoCambioEstado e) {
        return e.nuevo() != EstadoPedido.PENDIENTE_PAGO
                && !(e.nuevo() == EstadoPedido.CANCELADO && e.anterior() == EstadoPedido.PENDIENTE_PAGO);
    }

    /** El stock se reserva al registrar y vuelve al cancelar o rechazar. */
    private static boolean cambiaStock(EstadoPedido nuevo) {
        return nuevo == EstadoPedido.PENDIENTE_PAGO || nuevo == EstadoPedido.CANCELADO
                || nuevo == EstadoPedido.RECHAZADO;
    }

    private static EventoPedido eventoPedido(Pedido p, PedidoCambioEstado e) {
        return new EventoPedido("PEDIDO", p.getId(), p.getCodigo(), e.anterior(), e.nuevo(), e.canal(),
                p.getProductor().getId(), p.getProductor().getNombreComercial(), p.getComprador().getNombre(),
                p.getTotal(), p.getMontoProductor(), p.getActualizadoEn());
    }

    private static List<EventoStock> stock(Pedido p) {
        return p.getDetalles().stream().map(d -> d.getFormato().getProducto()).distinct()
                .map(ConsultaTiempoReal::stockDe).toList();
    }

    private static EventoStock stockDe(Producto producto) {
        return new EventoStock("STOCK", producto.getId(), producto.formatosActivos().stream()
                .map(ConsultaTiempoReal::formato).toList());
    }

    private static EventoStock.Formato formato(FormatoVenta f) {
        return new EventoStock.Formato(f.getId(), f.getNombre(), f.getStock(), f.disponible());
    }
}
