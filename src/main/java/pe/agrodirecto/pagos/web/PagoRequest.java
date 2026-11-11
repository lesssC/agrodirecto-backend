package pe.agrodirecto.pagos.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.agrodirecto.pagos.dominio.MetodoPago;

/**
 * Pago simulado. En producción esta información llegaría de la pasarela (Culqi, Mercado Pago, Niubiz)
 * mediante un webhook firmado, no del navegador del comprador.
 */
public record PagoRequest(@NotNull MetodoPago metodo, @Size(max = 40) String numeroOperacion) {
}
