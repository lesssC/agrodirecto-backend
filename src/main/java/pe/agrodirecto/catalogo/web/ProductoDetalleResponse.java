package pe.agrodirecto.catalogo.web;

import java.time.LocalDate;
import java.util.List;

import pe.agrodirecto.catalogo.dominio.Producto;
import pe.agrodirecto.productores.web.ProductorPublicoResponse;

/**
 * Ficha del producto para el comprador. Incluye el precio de referencia del mercado (RN-A2) para que compare:
 * lo que paga aquí llega íntegro al agricultor.
 */
public record ProductoDetalleResponse(Long id, String nombre, String categoria, String descripcion, String imagenUrl,
        boolean organico, LocalDate fechaCosecha, Integer vidaUtilDias, String calibre, boolean disponible,
        List<FormatoResponse> formatos, PrecioReferenciaResponse precioReferencia,
        ProductorPublicoResponse productor) {

    public static ProductoDetalleResponse de(Producto p) {
        return new ProductoDetalleResponse(p.getId(), p.getNombre(), p.getCategoria().getNombre(), p.getDescripcion(),
                p.getImagenUrl(), p.isOrganico(), p.getFechaCosecha(), p.getVidaUtilDias(), p.getCalibre(),
                p.disponible(), p.formatosActivos().stream().map(FormatoResponse::de).toList(),
                PrecioReferenciaResponse.de(p.getPrecioReferencia()), ProductorPublicoResponse.de(p.getProductor()));
    }
}
