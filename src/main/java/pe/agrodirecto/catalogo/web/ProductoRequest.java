package pe.agrodirecto.catalogo.web;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta o edición de un producto. Con {@code publicar = false} se guarda como borrador.
 * La imagen es una ruta pública ("/images/...") o una URL https; la subida de archivos llegará con el almacenamiento externo.
 */
public record ProductoRequest(
        @NotNull Long productorId,
        @NotNull Long categoriaId,
        Long precioReferenciaId,
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 1000) String descripcion,
        @Size(max = 500) @Pattern(regexp = "(/|https://).*", message = "debe ser una ruta /... o una URL https") String imagenUrl,
        Boolean organico,
        LocalDate fechaCosecha,
        @Min(1) @Max(3650) Integer vidaUtilDias,
        @Size(max = 60) String calibre,
        Boolean publicar,
        @NotEmpty(message = "agrega al menos una presentación de venta") @Size(max = 6) List<@Valid FormatoRequest> formatos) {

    /** Si el frontend no envía estos indicadores, se asumen en {@code false}. */
    public ProductoRequest {
        organico = Boolean.TRUE.equals(organico);
        publicar = Boolean.TRUE.equals(publicar);
    }
}
