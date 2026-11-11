package pe.agrodirecto.catalogo.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.catalogo.dominio.FormatoVenta;
import pe.agrodirecto.catalogo.dominio.PrecioReferencia;
import pe.agrodirecto.catalogo.dominio.Producto;
import pe.agrodirecto.compartido.Dinero;
import pe.agrodirecto.config.AgroDirectoProperties;

/**
 * RN-A3: avisa al agricultor cuando una presentación queda muy por debajo del precio mayorista de referencia.
 * No bloquea la publicación: el agricultor decide, pero decide informado y no por desconocimiento.
 */
@Service
@RequiredArgsConstructor
public class AlertaPrecioService {

    private final AgroDirectoProperties propiedades;

    public List<String> evaluar(Producto producto) {
        PrecioReferencia referencia = producto.getPrecioReferencia();
        List<String> alertas = new ArrayList<>();
        if (referencia == null) {
            return alertas;
        }
        BigDecimal mayorista = referencia.getPrecioMayoristaKg();
        BigDecimal factor = BigDecimal.ONE.subtract(
                propiedades.alertaPrecioBajoPorcentaje().divide(Dinero.CIEN, 4, RoundingMode.HALF_UP));
        BigDecimal umbral = mayorista.multiply(factor);

        for (FormatoVenta formato : producto.formatosActivos()) {
            formato.precioPorKg().filter(precioKg -> precioKg.compareTo(umbral) < 0).ifPresent(precioKg -> {
                BigDecimal diferencia = BigDecimal.ONE.subtract(precioKg.divide(mayorista, 4, RoundingMode.HALF_UP))
                        .multiply(Dinero.CIEN).setScale(0, RoundingMode.HALF_UP);
                alertas.add("«" + formato.getNombre() + "» queda a S/ " + precioKg + " por kg, " + diferencia
                        + "% debajo del precio mayorista de referencia de " + referencia.getNombre().toLowerCase()
                        + " (S/ " + mayorista + "/kg). Revisa si cubre tus costos antes de publicar.");
            });
        }
        return alertas;
    }
}
