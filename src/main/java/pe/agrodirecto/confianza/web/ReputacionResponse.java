package pe.agrodirecto.confianza.web;

import java.math.BigDecimal;

/**
 * RN-21: reputación medible del productor.
 *
 * @param tasaCumplimiento porcentaje de pedidos cumplidos sobre los que aceptó o debió atender; {@code null} sin historial
 * @param nivel            NUEVO, CONFIABLE, REGULAR o EN_OBSERVACION
 */
public record ReputacionResponse(Long productorId, BigDecimal promedio, long totalResenas, long pedidosCompletados,
        long pedidosRechazados, long disputasPerdidas, Integer tasaCumplimiento, String nivel, String descripcion) {
}
