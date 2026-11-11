package pe.agrodirecto.notificaciones.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SmsEntranteRequest(@NotBlank String telefono, @NotBlank @Size(max = 160) String mensaje) {
}
