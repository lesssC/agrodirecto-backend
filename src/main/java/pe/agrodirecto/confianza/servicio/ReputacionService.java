package pe.agrodirecto.confianza.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.confianza.dominio.EstadoDisputa;
import pe.agrodirecto.confianza.repositorio.DisputaRepository;
import pe.agrodirecto.confianza.repositorio.ResenaRepository;
import pe.agrodirecto.confianza.web.ReputacionResponse;
import pe.agrodirecto.pedidos.dominio.EstadoPedido;
import pe.agrodirecto.pedidos.repositorio.PedidoRepository;
import pe.agrodirecto.productores.servicio.ProductorService;

/**
 * RN-21: reputación calculada con hechos (pedidos cumplidos, rechazos y disputas perdidas), no solo con estrellas.
 * Un productor nuevo no se castiga por no tener historial: se muestra como NUEVO.
 */
@Service
@RequiredArgsConstructor
public class ReputacionService {

    static final int VENTAS_PARA_EVALUAR = 3;

    private final ResenaRepository resenas;
    private final DisputaRepository disputas;
    private final PedidoRepository pedidos;
    private final ProductorService productorService;

    @Transactional(readOnly = true)
    public ReputacionResponse de(Long productorId) {
        productorService.buscar(productorId);
        Double promedio = resenas.promedio(productorId);
        long completados = pedidos.countByProductorIdAndEstado(productorId, EstadoPedido.COMPLETADO);
        long rechazados = pedidos.countByProductorIdAndEstado(productorId, EstadoPedido.RECHAZADO);
        long perdidas = disputas.countByPedidoProductorIdAndEstado(productorId, EstadoDisputa.A_FAVOR_COMPRADOR);

        long evaluados = completados + rechazados + perdidas;
        Integer tasa = evaluados == 0 ? null : (int) Math.round(100.0 * completados / evaluados);
        String[] nivel = nivel(completados, tasa);
        return new ReputacionResponse(productorId,
                promedio == null ? null : BigDecimal.valueOf(promedio).setScale(1, RoundingMode.HALF_UP),
                resenas.countByProductorId(productorId), completados, rechazados, perdidas, tasa, nivel[0], nivel[1]);
    }

    /** Para el directorio y el catálogo: varias reputaciones en una sola llamada. */
    @Transactional(readOnly = true)
    public List<ReputacionResponse> de(List<Long> productorIds) {
        return productorIds.stream().distinct().map(this::de).toList();
    }

    private static String[] nivel(long completados, Integer tasa) {
        if (completados < VENTAS_PARA_EVALUAR) {
            return new String[] {"NUEVO", "Aún tiene pocas ventas en AgroDirecto."};
        }
        if (tasa >= 90) {
            return new String[] {"CONFIABLE", "Cumple " + tasa + "% de sus pedidos."};
        }
        if (tasa >= 70) {
            return new String[] {"REGULAR", "Cumple " + tasa + "% de sus pedidos."};
        }
        return new String[] {"EN_OBSERVACION", "Cumple solo " + tasa + "% de sus pedidos; AgroDirecto revisa su caso."};
    }
}
