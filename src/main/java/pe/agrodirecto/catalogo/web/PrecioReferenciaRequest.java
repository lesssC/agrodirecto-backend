package pe.agrodirecto.catalogo.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PrecioReferenciaRequest(
        @NotBlank @Pattern(regexp = "[A-Z_]{2,60}", message = "usa MAYÚSCULAS y guion bajo, p. ej. PAPA_AMARILLA") String clave,
        @NotBlank @Size(max = 120) String nombre,
        @NotNull @DecimalMin("0.01") BigDecimal precioMayoristaKg,
        @NotNull @DecimalMin("0.01") BigDecimal precioMinoristaKg,
        @NotBlank @Size(max = 160) String fuente,
        @NotNull LocalDate fecha) {
}
