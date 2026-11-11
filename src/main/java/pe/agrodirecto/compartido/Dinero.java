package pe.agrodirecto.compartido;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Utilidades para montos en soles: siempre dos decimales con redondeo comercial. */
public final class Dinero {

    public static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private Dinero() {
    }

    public static BigDecimal redondear(BigDecimal monto) {
        return monto.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal porcentaje(BigDecimal monto, BigDecimal porcentaje) {
        return redondear(monto.multiply(porcentaje).divide(CIEN, 4, RoundingMode.HALF_UP));
    }
}
