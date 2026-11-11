package pe.agrodirecto.administracion.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Toda sanción necesita un motivo: queda en la auditoría. */
public record MotivoAdminRequest(@NotBlank @Size(max = 400) String motivo) {
}
