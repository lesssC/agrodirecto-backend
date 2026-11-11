package pe.agrodirecto.pagos.servicio;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.compartido.error.NoEncontradoException;
import pe.agrodirecto.pagos.dominio.MetodoPago;
import pe.agrodirecto.pagos.dominio.Pago;
import pe.agrodirecto.pagos.repositorio.PagoRepository;

/** Operaciones sobre el dinero de un pedido. Las invoca el módulo de pedidos en cada cambio de estado. */
@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagos;
    private final Clock reloj;

    @Transactional
    public Pago crearPendiente(Long pedidoId, BigDecimal monto) {
        Pago pago = new Pago();
        pago.setPedidoId(pedidoId);
        pago.setMonto(monto);
        pago.setCreadoEn(ahora());
        return pagos.save(pago);
    }

    /**
     * Simula la confirmación de la pasarela. Si el comprador no indica número de operación,
     * se genera una referencia de simulación.
     */
    @Transactional
    public Pago retener(Long pedidoId, MetodoPago metodo, String numeroOperacion) {
        Pago pago = buscar(pedidoId);
        String referencia = numeroOperacion != null && !numeroOperacion.isBlank() ? numeroOperacion.trim()
                : "SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        pago.retener(metodo, referencia, ahora());
        return pago;
    }

    @Transactional
    public Pago liberar(Long pedidoId, Pago.Liquidacion liquidacion) {
        Pago pago = buscar(pedidoId);
        pago.liberar(liquidacion, ahora());
        return pago;
    }

    @Transactional
    public Pago revertir(Long pedidoId) {
        Pago pago = buscar(pedidoId);
        pago.revertir(ahora());
        return pago;
    }

    @Transactional(readOnly = true)
    public Pago buscar(Long pedidoId) {
        return pagos.findByPedidoId(pedidoId).orElseThrow(() -> new NoEncontradoException("Pago del pedido", pedidoId));
    }

    /** Para listar muchos pedidos con una sola consulta de pagos. */
    @Transactional(readOnly = true)
    public Map<Long, Pago> porPedido(Collection<Long> pedidoIds) {
        return pagos.findByPedidoIdIn(pedidoIds).stream()
                .collect(Collectors.toMap(Pago::getPedidoId, Function.identity()));
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }
}
