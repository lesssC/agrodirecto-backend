package pe.agrodirecto.catalogo.web;

import pe.agrodirecto.catalogo.dominio.Categoria;

public record CategoriaResponse(Long id, String nombre) {

    public static CategoriaResponse de(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNombre());
    }
}
