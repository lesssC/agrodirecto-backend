package pe.agrodirecto.catalogo.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.agrodirecto.catalogo.dominio.TipoFormato;

/** Presentación de venta. Con {@code id} se actualiza una existente; sin él se crea una nueva. */
public record FormatoRequest(
        Long id,
        @NotBlank @Size(max = 80) String nombre,
        @NotNull TipoFormato tipo,
        @DecimalMin(value = "0.001", message = "debe ser mayor que 0") BigDecimal pesoKg,
        @NotNull @DecimalMin(value = "0.10", message = "debe ser al menos S/ 0.10") BigDecimal precio,
        @NotNull @Min(0) Integer stock,
        @Min(value = 2, message = "el descuento por volumen aplica desde 2 unidades") Integer cantidadMinDescuento,
        @DecimalMin("1") @DecimalMax(value = "30", message = "no puede superar el 30%") BigDecimal porcentajeDescuento) {
}
