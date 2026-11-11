package pe.agrodirecto.catalogo.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import pe.agrodirecto.catalogo.dominio.FormatoVenta;
import pe.agrodirecto.catalogo.dominio.Producto;
import pe.agrodirecto.productores.dominio.Productor;

/** Tarjeta del catálogo: lo justo para listar y comparar. */
public record ProductoResumenResponse(Long id, String nombre, String categoria, String imagenUrl, boolean organico,
        LocalDate fechaCosecha, BigDecimal precioDesde, String formatoDesde, boolean disponible,
        ProductorBreve productor) {

    public record ProductorBreve(Long id, String nombreComercial, String region, String provincia,
            boolean verificado) {

        static ProductorBreve de(Productor p) {
            return new ProductorBreve(p.getId(), p.getNombreComercial(), p.getRegion(), p.getProvincia(),
                    p.isVerificado());
        }
    }

    public static ProductoResumenResponse de(Producto p) {
        return new ProductoResumenResponse(p.getId(), p.getNombre(), p.getCategoria().getNombre(), p.getImagenUrl(),
                p.isOrganico(), p.getFechaCosecha(), p.precioDesde().orElse(null),
                p.formatoDesde().map(FormatoVenta::getNombre).orElse(null), p.disponible(),
                ProductorBreve.de(p.getProductor()));
    }
}
