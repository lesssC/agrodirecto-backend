package pe.agrodirecto.confianza.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResenaRequest(@NotNull @Min(1) @Max(5) Integer calificacion, @Size(max = 500) String comentario) {
}
