package pe.agrodirecto.pagos.dominio;

public enum EstadoPago {
    /** El pedido se registró pero el comprador aún no paga. */
    PENDIENTE,
    /** RN-C1: el comprador pagó; la plataforma guarda el dinero hasta la entrega. */
    RETENIDO,
    /** RN-C2: se transfirió al agricultor. */
    LIBERADO,
    /** Se devolvió al comprador (pedido rechazado, cancelado o disputa a su favor). */
    DEVUELTO,
    /** El pedido se canceló antes de pagar: no hubo movimiento de dinero. */
    ANULADO
}
