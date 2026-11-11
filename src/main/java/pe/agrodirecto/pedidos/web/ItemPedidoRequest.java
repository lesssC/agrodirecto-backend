package pe.agrodirecto.pedidos.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Solo presentación y cantidad: el precio lo pone el servidor (RN-A1). */
public record ItemPedidoRequest(@NotNull Long formatoId, @NotNull @Min(1) @Max(1000) Integer cantidad) {
}
