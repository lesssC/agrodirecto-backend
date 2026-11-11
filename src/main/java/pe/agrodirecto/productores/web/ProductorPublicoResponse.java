package pe.agrodirecto.productores.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import pe.agrodirecto.productores.dominio.Productor;

/**
 * Lo que cualquier visitante ve del agricultor: su historia, su zona y sus condiciones de venta.
 * No incluye DNI, celular ni datos de cobro (RN-24), y la ubicación se redondea a ~1 km (RN-06).
 */
public record ProductorPublicoResponse(Long id, String nombreComercial, String nombreTitular, String region,
        String provincia, String distrito, BigDecimal latitud, BigDecimal longitud, String historia,
        String practicas, boolean verificado, boolean emiteFactura, AsociacionResponse asociacion,
        List<ZonaResponse> zonas) {

    public static ProductorPublicoResponse de(Productor p) {
        return new ProductorPublicoResponse(p.getId(), p.getNombreComercial(), p.getNombreTitular(), p.getRegion(),
                p.getProvincia(), p.getDistrito(), aproximar(p.getLatitud()), aproximar(p.getLongitud()),
                p.getHistoria(), p.getPracticas(), p.isVerificado(), p.emiteFactura(),
                AsociacionResponse.de(p.getAsociacion()), p.getZonas().stream().map(ZonaResponse::de).toList());
    }

    private static BigDecimal aproximar(BigDecimal coordenada) {
        return coordenada == null ? null : coordenada.setScale(2, RoundingMode.HALF_UP);
    }
}
