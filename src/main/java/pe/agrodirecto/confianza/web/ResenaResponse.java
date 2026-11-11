package pe.agrodirecto.confianza.web;

import java.time.LocalDateTime;

import pe.agrodirecto.confianza.dominio.Resena;

public record ResenaResponse(Long id, String pedido, String autor, int calificacion, String comentario,
        LocalDateTime creadoEn) {

    public static ResenaResponse de(Resena r) {
        return new ResenaResponse(r.getId(), r.getPedido().getCodigo(), r.getAutor().getNombre(), r.getCalificacion(),
                r.getComentario(), r.getCreadoEn());
    }
}
