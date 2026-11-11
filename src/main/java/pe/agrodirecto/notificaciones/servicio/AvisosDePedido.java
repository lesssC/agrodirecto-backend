package pe.agrodirecto.notificaciones.servicio;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.config.AgroDirectoProperties;
import pe.agrodirecto.pedidos.dominio.CanalOperacion;
import pe.agrodirecto.pedidos.dominio.EstadoPedido;
import pe.agrodirecto.pedidos.dominio.Pedido;
import pe.agrodirecto.pedidos.dominio.PedidoCambioEstado;
import pe.agrodirecto.pedidos.repositorio.PedidoRepository;
import pe.agrodirecto.productores.dominio.Productor;

/**
 * Escucha los cambios de estado de los pedidos y decide a quién avisar y con qué palabras.
 * Corre dentro de la misma transacción: si el cambio falla, tampoco se registra el aviso.
 */
@Component
@RequiredArgsConstructor
public class AvisosDePedido {

    static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM");

    private final PedidoRepository pedidos;
    private final NotificacionService notificaciones;
    private final AgroDirectoProperties propiedades;

    @EventListener
    public void alCambiarEstado(PedidoCambioEstado evento) {
        Pedido pedido = pedidos.findById(evento.pedidoId()).orElse(null);
        if (pedido == null) {
            return;
        }
        Productor productor = pedido.getProductor();
        String codigo = pedido.getCodigo();
        String agricultor = productor.getNombreComercial();

        switch (evento.nuevo()) {
            case PENDIENTE -> notificaciones.avisarAlProductor(productor, pedido.getId(),
                    "AgroDirecto: nuevo pedido " + codigo + " de " + pedido.getComprador().getNombre() + ": "
                            + resumen(pedido) + ". Recibirás S/ " + pedido.getMontoProductor()
                            + " (pago asegurado). Entrega en " + pedido.getRegionDestino() + " hasta el "
                            + pedido.getFechaEntregaEstimada().format(FECHA) + ". Responde 1 " + codigo
                            + " para ACEPTAR o 2 " + codigo + " para RECHAZAR.");
            case ACEPTADO -> alComprador(pedido, agricultor + " aceptó tu pedido " + codigo
                    + ". Ya puedes ver su celular para coordinar la entrega.");
            case RECHAZADO -> alComprador(pedido, agricultor + " no puede atender tu pedido " + codigo
                    + ". Te devolvemos S/ " + pedido.getTotal() + ".");
            case EN_PREPARACION -> alComprador(pedido, agricultor + " está preparando tu pedido " + codigo + ".");
            case LISTO_PARA_ENTREGA -> alComprador(pedido, "Tu pedido " + codigo + " está listo para salir.");
            case ENTREGADO -> alComprador(pedido, agricultor + " marcó tu pedido " + codigo
                    + " como entregado. Confirma la recepción o reporta un problema dentro de "
                    + propiedades.horasLiberacionAutomatica() + " horas.");
            case COMPLETADO -> notificaciones.avisarAlProductor(productor, pedido.getId(),
                    "AgroDirecto: pago liberado. S/ " + pedido.getMontoProductor() + " del pedido " + codigo
                            + " van a tu " + productor.getMetodoCobro().name().replace('_', ' ') + " terminado en "
                            + ultimos3(productor.getNumeroCobro()) + ". Gracias por vender en AgroDirecto.");
            case CANCELADO -> cancelado(evento, pedido, productor);
            default -> {
                // PENDIENTE_PAGO: el pedido aún no se pagó; nadie más necesita enterarse.
            }
        }
    }

    private void cancelado(PedidoCambioEstado evento, Pedido pedido, Productor productor) {
        // Solo se avisa al agricultor si ya había visto el pedido (estaba pagado).
        if (evento.anterior() == EstadoPedido.PENDIENTE) {
            notificaciones.avisarAlProductor(productor, pedido.getId(), "AgroDirecto: el comprador canceló el pedido "
                    + pedido.getCodigo() + ". No lo prepares.");
        }
        if (evento.canal() == CanalOperacion.SISTEMA) {
            alComprador(pedido, "Tu pedido " + pedido.getCodigo() + " se canceló porque no se pagó a tiempo.");
        }
    }

    private void alComprador(Pedido pedido, String mensaje) {
        notificaciones.avisarAlUsuario(pedido.getComprador(), pedido.getId(), mensaje);
    }

    static String resumen(Pedido pedido) {
        return pedido.getDetalles().stream()
                .map(d -> d.getCantidad() + " x " + d.getFormato().getNombre() + " de "
                        + d.getFormato().getProducto().getNombre())
                .collect(Collectors.joining(", "));
    }

    private static String ultimos3(String numero) {
        return numero.length() <= 3 ? numero : numero.substring(numero.length() - 3);
    }
}
