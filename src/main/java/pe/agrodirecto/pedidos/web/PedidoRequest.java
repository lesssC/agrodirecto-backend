package pe.agrodirecto.pedidos.web;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.agrodirecto.pedidos.dominio.ModalidadEntrega;

public record PedidoRequest(
        @NotEmpty(message = "el carrito está vacío") @Size(max = 30) List<@Valid ItemPedidoRequest> items,
        @NotNull ModalidadEntrega modalidadEntrega,
        @NotBlank String regionDestino,
        @NotBlank @Size(max = 250) String direccionEntrega,
        @Size(max = 500) String nota) {
}
