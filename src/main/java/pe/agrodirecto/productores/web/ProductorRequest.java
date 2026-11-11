package pe.agrodirecto.productores.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.agrodirecto.productores.dominio.MetodoCobro;

/** Datos del perfil del agricultor. Lo llena él mismo o su agente de campo. */
public record ProductorRequest(
        @NotBlank @Size(max = 120) String nombreComercial,
        @NotBlank @Size(max = 120) String nombreTitular,
        @NotBlank @Pattern(regexp = "\\d{8}", message = "debe tener 8 dígitos") String dni,
        @NotBlank @Pattern(regexp = "9\\d{8}", message = "debe ser un celular de 9 dígitos") String telefono,
        @NotBlank String region,
        @NotBlank @Size(max = 60) String provincia,
        @Size(max = 60) String distrito,
        BigDecimal latitud,
        BigDecimal longitud,
        @Size(max = 1000) String historia,
        @Size(max = 500) String practicas,
        @Pattern(regexp = "(10|20)\\d{9}", message = "debe tener 11 dígitos y empezar con 10 o 20") String ruc,
        @NotNull MetodoCobro metodoCobro,
        @NotBlank String numeroCobro,
        Long asociacionId) {
}
