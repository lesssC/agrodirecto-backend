package pe.agrodirecto.productores.web;

import java.math.BigDecimal;

import pe.agrodirecto.productores.dominio.ZonaCobertura;

public record ZonaResponse(String region, int diasTransito, BigDecimal pedidoMinimo, BigDecimal costoFlete) {

    public static ZonaResponse de(ZonaCobertura z) {
        return new ZonaResponse(z.getRegion(), z.getDiasTransito(), z.getPedidoMinimo(), z.getCostoFlete());
    }
}
