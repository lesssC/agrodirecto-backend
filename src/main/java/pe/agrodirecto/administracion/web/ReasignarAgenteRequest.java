package pe.agrodirecto.administracion.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReasignarAgenteRequest(@NotNull Long agenteId, @NotBlank @Size(max = 400) String motivo) {
}
