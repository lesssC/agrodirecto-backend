package pe.agrodirecto.productores.web;

import pe.agrodirecto.productores.dominio.Asociacion;

public record AsociacionResponse(Long id, String nombre, String region, boolean emiteFactura) {

    public static AsociacionResponse de(Asociacion a) {
        return a == null ? null : new AsociacionResponse(a.getId(), a.getNombre(), a.getRegion(), a.isEmiteFactura());
    }
}
