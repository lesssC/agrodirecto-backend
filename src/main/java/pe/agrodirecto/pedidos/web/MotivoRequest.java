package pe.agrodirecto.pedidos.web;

import jakarta.validation.constraints.Size;

public record MotivoRequest(@Size(max = 300) String motivo) {
}
