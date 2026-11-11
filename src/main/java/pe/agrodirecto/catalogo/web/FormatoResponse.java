package pe.agrodirecto.catalogo.web;

import java.math.BigDecimal;

import pe.agrodirecto.catalogo.dominio.FormatoVenta;
import pe.agrodirecto.catalogo.dominio.TipoFormato;

public record FormatoResponse(Long id, String nombre, TipoFormato tipo, BigDecimal pesoKg, BigDecimal precio,
        BigDecimal precioPorKg, int stock, boolean disponible, boolean activo, Descuento descuento) {

    /** Descuento por volumen que ofrece el agricultor (RN-A1). */
    public record Descuento(int desdeCantidad, BigDecimal porcentaje) {
    }

    public static FormatoResponse de(FormatoVenta f) {
        Descuento descuento = f.getCantidadMinDescuento() == null ? null
                : new Descuento(f.getCantidadMinDescuento(), f.getPorcentajeDescuento());
        return new FormatoResponse(f.getId(), f.getNombre(), f.getTipo(), f.getPesoKg(), f.getPrecio(),
                f.precioPorKg().orElse(null), f.getStock(), f.disponible(), f.isActivo(), descuento);
    }
}
