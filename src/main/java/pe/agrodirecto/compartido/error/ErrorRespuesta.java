package pe.agrodirecto.compartido.error;

import java.time.LocalDateTime;
import java.util.List;

/** Formato único de error que recibe el frontend. {@code regla} indica la regla de negocio incumplida, si aplica. */
public record ErrorRespuesta(int estado, String regla, String mensaje, List<String> detalles, LocalDateTime fecha) {

    public static ErrorRespuesta de(int estado, String regla, String mensaje, List<String> detalles) {
        return new ErrorRespuesta(estado, regla, mensaje, detalles, LocalDateTime.now());
    }
}
