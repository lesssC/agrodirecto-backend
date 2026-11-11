package pe.agrodirecto.pedidos.dominio;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Ciclo de vida del pedido (RN-05). Cada estado declara a cuáles puede pasar; cualquier otro salto se rechaza.
 */
public enum EstadoPedido {
    /** Registrado con el stock reservado; espera el pago del comprador. El agricultor aún no lo ve. */
    PENDIENTE_PAGO,
    /** Pagado (dinero retenido); espera que el agricultor lo acepte o rechace. */
    PENDIENTE,
    ACEPTADO,
    EN_PREPARACION,
    LISTO_PARA_ENTREGA,
    /** El agricultor lo entregó; el comprador debe confirmar la recepción. */
    ENTREGADO,
    /** El comprador confirmó que recibió conforme. */
    COMPLETADO,
    /** RN-22: el comprador reportó un problema; el pago queda congelado hasta que la administración resuelva. */
    EN_DISPUTA,
    /** Disputa resuelta a favor del comprador: se le devolvió el dinero. */
    REEMBOLSADO,
    RECHAZADO,
    CANCELADO;

    private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES = Map.of(
            PENDIENTE_PAGO, EnumSet.of(PENDIENTE, CANCELADO),
            PENDIENTE, EnumSet.of(ACEPTADO, RECHAZADO, CANCELADO),
            ACEPTADO, EnumSet.of(EN_PREPARACION),
            EN_PREPARACION, EnumSet.of(LISTO_PARA_ENTREGA),
            LISTO_PARA_ENTREGA, EnumSet.of(ENTREGADO),
            ENTREGADO, EnumSet.of(COMPLETADO, EN_DISPUTA),
            EN_DISPUTA, EnumSet.of(COMPLETADO, REEMBOLSADO));

    public boolean puedePasarA(EstadoPedido siguiente) {
        return TRANSICIONES.getOrDefault(this, Set.of()).contains(siguiente);
    }

    /** Pedido en curso: cuenta para el límite de pedidos abiertos de una cuenta nueva (RN-18). */
    public boolean esAbierto() {
        return abiertos().contains(this);
    }

    /**
     * RN-24: el celular y la dirección exacta solo se revelan cuando el agricultor ya aceptó el pedido.
     * Así se protege la privacidad y se evita que el trato se cierre fuera de la plataforma.
     */
    public boolean revelaContacto() {
        return EnumSet.of(ACEPTADO, EN_PREPARACION, LISTO_PARA_ENTREGA, ENTREGADO, COMPLETADO, EN_DISPUTA, REEMBOLSADO)
                .contains(this);
    }

    public static Set<EstadoPedido> abiertos() {
        return EnumSet.of(PENDIENTE_PAGO, PENDIENTE, ACEPTADO, EN_PREPARACION, LISTO_PARA_ENTREGA, ENTREGADO,
                EN_DISPUTA);
    }
}
