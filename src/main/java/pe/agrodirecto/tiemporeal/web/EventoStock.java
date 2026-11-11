package pe.agrodirecto.tiemporeal.web;

import java.util.List;

/** Stock actualizado de un producto: el catálogo abierto muestra al instante cuánto queda. */
public record EventoStock(String tipo, Long productoId, List<Formato> formatos) {

    public record Formato(Long formatoId, String nombre, int stock, boolean disponible) {
    }
}
