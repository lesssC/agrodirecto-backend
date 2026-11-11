package pe.agrodirecto.usuarios.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.agrodirecto.usuarios.dominio.Rol;

public record RegistroRequest(
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres") String password,
        @NotBlank @Pattern(regexp = "9\\d{8}", message = "debe ser un celular peruano de 9 dígitos") String telefono,
        @Pattern(regexp = "\\d{8}", message = "debe tener 8 dígitos") String dni,
        @Pattern(regexp = "(10|20)\\d{9}", message = "debe tener 11 dígitos y empezar con 10 o 20") String ruc,
        @Size(max = 160) String razonSocial,
        @NotNull Rol rol) {
}
