package pe.agrodirecto.administracion.web;

import java.time.LocalDateTime;

import pe.agrodirecto.administracion.dominio.AccionAdministrativa;
import pe.agrodirecto.administracion.dominio.TipoAccion;

public record AccionResponse(Long id, String administrador, TipoAccion tipo, String objetivo, Long objetivoId,
        String detalle, LocalDateTime fecha) {

    public static AccionResponse de(AccionAdministrativa a) {
        return new AccionResponse(a.getId(), a.getAdministrador().getNombre(), a.getTipo(), a.getObjetivo(),
                a.getObjetivoId(), a.getDetalle(), a.getCreadoEn());
    }
}
