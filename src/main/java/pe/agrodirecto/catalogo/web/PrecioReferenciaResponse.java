package pe.agrodirecto.catalogo.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import pe.agrodirecto.catalogo.dominio.PrecioReferencia;

public record PrecioReferenciaResponse(Long id, String clave, String nombre, BigDecimal precioMayoristaKg,
        BigDecimal precioMinoristaKg, String fuente, LocalDate fecha) {

    public static PrecioReferenciaResponse de(PrecioReferencia r) {
        return r == null ? null : new PrecioReferenciaResponse(r.getId(), r.getClave(), r.getNombre(),
                r.getPrecioMayoristaKg(), r.getPrecioMinoristaKg(), r.getFuente(), r.getFecha());
    }
}
