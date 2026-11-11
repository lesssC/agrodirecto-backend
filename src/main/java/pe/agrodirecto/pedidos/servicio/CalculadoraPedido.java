package pe.agrodirecto.pedidos.servicio;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.catalogo.dominio.FormatoVenta;
import pe.agrodirecto.compartido.Dinero;
import pe.agrodirecto.config.AgroDirectoProperties;

/**
 * Calcula los montos de un pedido a partir de los precios guardados en la base. Es la única fuente de verdad:
 * el comprador nunca envía precios (RN-A1).
 *
 * <pre>
 * neto            = Σ precio × cantidad − descuento por volumen
 * comisión        = neto × % de servicio                (la paga el comprador, RN-A4)
 * total comprador = neto + flete + comisión
 * monto productor = neto + flete                       (recibe íntegro su precio)
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class CalculadoraPedido {

    private final AgroDirectoProperties propiedades;

    public record Linea(FormatoVenta formato, int cantidad) {
    }

    public record LineaCalculada(FormatoVenta formato, int cantidad, BigDecimal precioUnitario,
            BigDecimal porcentajeDescuento, BigDecimal neto) {
    }

    public record Resultado(List<LineaCalculada> lineas, BigDecimal subtotal, BigDecimal descuento, BigDecimal neto,
            BigDecimal costoFlete, BigDecimal comision, BigDecimal total, BigDecimal montoProductor) {
    }

    public Resultado calcular(List<Linea> lineas, BigDecimal costoFlete) {
        List<LineaCalculada> calculadas = lineas.stream().map(this::calcularLinea).toList();
        BigDecimal subtotal = Dinero.redondear(calculadas.stream()
                .map(l -> l.precioUnitario().multiply(BigDecimal.valueOf(l.cantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal neto = Dinero.redondear(calculadas.stream().map(LineaCalculada::neto)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal flete = Dinero.redondear(costoFlete);
        BigDecimal comision = Dinero.porcentaje(neto, propiedades.comisionPorcentaje());
        return new Resultado(calculadas, subtotal, subtotal.subtract(neto), neto, flete, comision,
                neto.add(flete).add(comision), neto.add(flete));
    }

    private LineaCalculada calcularLinea(Linea linea) {
        FormatoVenta formato = linea.formato();
        BigDecimal bruto = formato.getPrecio().multiply(BigDecimal.valueOf(linea.cantidad()));
        BigDecimal porcentaje = formato.porcentajeDescuentoPara(linea.cantidad());
        BigDecimal neto = Dinero.redondear(bruto.subtract(Dinero.porcentaje(bruto, porcentaje)));
        return new LineaCalculada(formato, linea.cantidad(), formato.getPrecio(), porcentaje, neto);
    }
}
