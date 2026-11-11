package pe.agrodirecto.confianza.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import pe.agrodirecto.confianza.dominio.Disputa;
import pe.agrodirecto.confianza.dominio.EstadoDisputa;
import pe.agrodirecto.confianza.dominio.MotivoDisputa;

public record DisputaResponse(Long id, Long pedidoId, String pedido, BigDecimal montoEnJuego, String productor,
        String comprador, MotivoDisputa motivo, String descripcion, String evidenciaUrl, String descargo,
        String evidenciaDescargoUrl, LocalDateTime descargoEn, EstadoDisputa estado, String resolucion,
        String resueltaPor, LocalDateTime creadaEn, LocalDateTime resueltaEn) {

    public static DisputaResponse de(Disputa d) {
        return new DisputaResponse(d.getId(), d.getPedido().getId(), d.getPedido().getCodigo(), d.getPedido().getTotal(),
                d.getPedido().getProductor().getNombreComercial(), d.getPedido().getComprador().getNombre(),
                d.getMotivo(), d.getDescripcion(), d.getEvidenciaUrl(), d.getDescargo(), d.getEvidenciaDescargoUrl(),
                d.getDescargoEn(), d.getEstado(), d.getResolucion(),
                d.getResueltaPor() == null ? null : d.getResueltaPor().getNombre(), d.getCreadaEn(), d.getResueltaEn());
    }
}
