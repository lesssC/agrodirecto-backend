package pe.agrodirecto.productores.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import pe.agrodirecto.productores.dominio.MetodoCobro;
import pe.agrodirecto.productores.dominio.Productor;
import pe.agrodirecto.usuarios.dominio.EstadoCuenta;
import pe.agrodirecto.usuarios.dominio.Usuario;

/** Vista completa del perfil, solo para quien lo gestiona (el agricultor, su agente o la administración). */
public record ProductorGestionResponse(Long id, String nombreComercial, String nombreTitular, String dni,
        String telefono, String region, String provincia, String distrito, BigDecimal latitud, BigDecimal longitud,
        String historia, String practicas, boolean tieneRuc, String ruc, boolean emiteFactura,
        MetodoCobro metodoCobro, String numeroCobro, AsociacionResponse asociacion, boolean tieneCuentaPropia,
        String agente, boolean verificado, String verificadoPor, LocalDateTime fechaVerificacion,
        EstadoCuenta estado, List<ZonaResponse> zonas) {

    public static ProductorGestionResponse de(Productor p) {
        return new ProductorGestionResponse(p.getId(), p.getNombreComercial(), p.getNombreTitular(), p.getDni(),
                p.getTelefono(), p.getRegion(), p.getProvincia(), p.getDistrito(), p.getLatitud(), p.getLongitud(),
                p.getHistoria(), p.getPracticas(), p.isTieneRuc(), p.getRuc(), p.emiteFactura(), p.getMetodoCobro(),
                p.getNumeroCobro(), AsociacionResponse.de(p.getAsociacion()), p.tieneCuentaPropia(),
                nombre(p.getAgente()), p.isVerificado(), nombre(p.getVerificadoPor()), p.getFechaVerificacion(),
                p.getEstado(), p.getZonas().stream().map(ZonaResponse::de).toList());
    }

    private static String nombre(Usuario u) {
        return u == null ? null : u.getNombre();
    }
}
