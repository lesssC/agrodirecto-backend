package pe.agrodirecto.administracion.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Panel de la administración: estado de la plataforma y su impacto en los agricultores. */
public record ResumenResponse(
        Map<String, Long> usuariosPorRol,
        Pendientes pendientesDeVerificacion,
        long productores,
        long productoresVerificados,
        long productosActivos,
        Map<String, Long> pedidosPorEstado,
        Dinero dinero,
        long disputasAbiertas,
        List<ProductorDestacado> topProductores) {

    public record Pendientes(long agentes, long compradores, long productores) {
    }

    /**
     * @param transado               total pagado por compradores en pedidos completados
     * @param pagadoAgricultores     lo que recibieron los agricultores (su precio íntegro + flete)
     * @param comisiones             ingreso de la plataforma
     * @param retenido               dinero en custodia de pedidos en curso o en disputa
     * @param porcentajeAlAgricultor parte del dinero transado que llegó al agricultor
     */
    public record Dinero(BigDecimal transado, BigDecimal pagadoAgricultores, BigDecimal comisiones,
            BigDecimal retenido, BigDecimal porcentajeAlAgricultor) {
    }

    public record ProductorDestacado(Long productorId, String nombreComercial, BigDecimal ventas, long pedidos) {
    }
}
