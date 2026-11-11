package pe.agrodirecto.catalogo.web;

import jakarta.validation.constraints.NotNull;
import pe.agrodirecto.catalogo.dominio.EstadoProducto;

public record EstadoProductoRequest(@NotNull EstadoProducto estado) {
}
