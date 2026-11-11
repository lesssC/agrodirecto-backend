package pe.agrodirecto.confianza.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.agrodirecto.confianza.dominio.MotivoDisputa;

public record DisputaRequest(
        @NotNull MotivoDisputa motivo,
        @NotBlank @Size(max = 1000) String descripcion,
        @Size(max = 500) @Pattern(regexp = "(/|https://).*", message = "debe ser una ruta /... o una URL https") String evidenciaUrl) {
}
