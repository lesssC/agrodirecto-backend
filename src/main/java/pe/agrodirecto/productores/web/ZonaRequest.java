package pe.agrodirecto.productores.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ZonaRequest(
        @NotBlank String region,
        @Min(0) @Max(30) int diasTransito,
        @NotNull @DecimalMin("0.00") BigDecimal pedidoMinimo,
        @NotNull @DecimalMin("0.00") BigDecimal costoFlete) {
}
