package pe.agrodirecto.tiemporeal.servicio;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.pedidos.dominio.PedidoCambioEstado;
import pe.agrodirecto.tiemporeal.config.Destinos;

/**
 * Escucha el mismo evento que las notificaciones, pero publica <b>después del commit</b>: así nunca se anuncia
 * en vivo un cambio que luego se revirtió.
 */
@Component
@RequiredArgsConstructor
public class PublicadorTiempoReal {

    private final ConsultaTiempoReal consulta;
    private final SimpMessagingTemplate mensajeria;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCambiarEstado(PedidoCambioEstado evento) {
        consulta.preparar(evento).ifPresent(envio -> {
            mensajeria.convertAndSendToUser(envio.compradorId().toString(), Destinos.MIS_PEDIDOS, envio.pedido());
            if (envio.visibleParaProductor()) {
                mensajeria.convertAndSend(Destinos.pedidosDelProductor(envio.productorId()), envio.pedido());
            }
            envio.stock().forEach(stock ->
                    mensajeria.convertAndSend(Destinos.stockDelProducto(stock.productoId()), stock));
        });
    }
}
