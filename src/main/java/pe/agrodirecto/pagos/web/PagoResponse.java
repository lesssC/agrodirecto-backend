package pe.agrodirecto.pagos.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import pe.agrodirecto.pagos.dominio.EstadoPago;
import pe.agrodirecto.pagos.dominio.MetodoPago;
import pe.agrodirecto.pagos.dominio.Pago;

/** Estado del dinero del pedido, con un mensaje en lenguaje simple. */
public record PagoResponse(EstadoPago estado, BigDecimal monto, MetodoPago metodo, String referencia,
        LocalDateTime pagadoEn, LocalDateTime liberadoEn, LocalDateTime devueltoEn, String destino, String mensaje) {

    public static PagoResponse de(Pago p) {
        String destino = p.getDestinoMetodo() == null ? null
                : p.getDestinoMetodo() + " " + enmascarar(p.getDestinoNumero());
        return new PagoResponse(p.getEstado(), p.getMonto(), p.getMetodo(), p.getReferencia(), p.getPagadoEn(),
                p.getLiberadoEn(), p.getDevueltoEn(), destino, mensaje(p, destino));
    }

    private static String mensaje(Pago p, String destino) {
        return switch (p.getEstado()) {
            case PENDIENTE -> "Falta pagar S/ " + p.getMonto() + ". El agricultor recibirá el pedido cuando pagues.";
            case RETENIDO -> "AgroDirecto guarda el pago de S/ " + p.getMonto()
                    + " hasta que se confirme la entrega.";
            case LIBERADO -> "Se transfirieron S/ " + p.getMontoProductor() + " al agricultor (" + destino + ").";
            case DEVUELTO -> "Se devolvieron S/ " + p.getMonto() + " al comprador.";
            case ANULADO -> "El pedido se canceló antes del pago; no hubo cobro.";
        };
    }

    /** Solo los 3 últimos dígitos: suficiente para que el agricultor reconozca su cuenta. */
    static String enmascarar(String numero) {
        if (numero == null || numero.length() <= 3) {
            return numero;
        }
        return "•".repeat(numero.length() - 3) + numero.substring(numero.length() - 3);
    }
}
