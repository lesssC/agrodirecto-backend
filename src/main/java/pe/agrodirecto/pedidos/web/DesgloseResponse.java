package pe.agrodirecto.pedidos.web;

import java.math.BigDecimal;

/**
 * RN-A5: transparencia del reparto. {@code mensaje} explica en una frase cuánto llega al agricultor
 * y cuánto es el servicio de la plataforma.
 */
public record DesgloseResponse(BigDecimal subtotal, BigDecimal descuento, BigDecimal costoFlete, BigDecimal comision,
        BigDecimal total, BigDecimal montoProductor, String mensaje) {

    public static DesgloseResponse de(BigDecimal subtotal, BigDecimal descuento, BigDecimal flete, BigDecimal comision,
            BigDecimal total, BigDecimal montoProductor, String productor) {
        String mensaje = "De los S/ " + total + " que pagas, S/ " + montoProductor + " van íntegros a " + productor
                + " (su precio" + (flete.signum() > 0 ? " más el flete" : "") + ") y S/ " + comision
                + " son el servicio de AgroDirecto.";
        return new DesgloseResponse(subtotal, descuento, flete, comision, total, montoProductor, mensaje);
    }
}
