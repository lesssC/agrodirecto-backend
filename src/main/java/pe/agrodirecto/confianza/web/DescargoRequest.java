package pe.agrodirecto.confianza.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Versión del agricultor (o de su agente en su nombre). */
public record DescargoRequest(
        @NotBlank @Size(max = 1000) String descargo,
        @Size(max = 500) @Pattern(regexp = "(/|https://).*", message = "debe ser una ruta /... o una URL https") String evidenciaUrl) {
}
