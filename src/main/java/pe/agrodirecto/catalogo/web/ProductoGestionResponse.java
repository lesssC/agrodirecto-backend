package pe.agrodirecto.catalogo.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import pe.agrodirecto.catalogo.dominio.EstadoProducto;
import pe.agrodirecto.catalogo.dominio.Producto;

/** Vista de quien gestiona el producto: incluye borradores, formatos inactivos y alertas de precio (RN-A3). */
public record ProductoGestionResponse(Long id, Long productorId, String productor, String nombre, Long categoriaId,
        String categoria, Long precioReferenciaId, String descripcion, String imagenUrl, boolean organico,
        LocalDate fechaCosecha, Integer vidaUtilDias, String calibre, EstadoProducto estado,
        LocalDateTime actualizadoEn, List<FormatoResponse> formatos, List<String> alertas) {

    public static ProductoGestionResponse de(Producto p, List<String> alertas) {
        return new ProductoGestionResponse(p.getId(), p.getProductor().getId(),
                p.getProductor().getNombreComercial(), p.getNombre(), p.getCategoria().getId(),
                p.getCategoria().getNombre(), p.getPrecioReferencia() == null ? null : p.getPrecioReferencia().getId(),
                p.getDescripcion(), p.getImagenUrl(), p.isOrganico(), p.getFechaCosecha(), p.getVidaUtilDias(),
                p.getCalibre(), p.getEstado(), p.getActualizadoEn(),
                p.getFormatos().stream().map(FormatoResponse::de).toList(), alertas);
    }
}
