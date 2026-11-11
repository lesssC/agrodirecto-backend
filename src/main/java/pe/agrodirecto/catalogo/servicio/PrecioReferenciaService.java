package pe.agrodirecto.catalogo.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.catalogo.dominio.PrecioReferencia;
import pe.agrodirecto.catalogo.repositorio.PrecioReferenciaRepository;
import pe.agrodirecto.catalogo.web.PrecioReferenciaRequest;
import pe.agrodirecto.catalogo.web.PrecioReferenciaResponse;
import pe.agrodirecto.compartido.Dinero;
import pe.agrodirecto.compartido.error.NoEncontradoException;
import pe.agrodirecto.compartido.error.ReglaNegocioException;

/**
 * Mantenimiento de los precios de mercado (RN-A2). Los actualiza la administración, por ejemplo con los
 * boletines de precios mayoristas, para que las alertas al agricultor reflejen el mercado real.
 */
@Service
@RequiredArgsConstructor
public class PrecioReferenciaService {

    private final PrecioReferenciaRepository precios;

    @Transactional
    public PrecioReferenciaResponse crear(PrecioReferenciaRequest solicitud) {
        if (precios.existsByClave(solicitud.clave())) {
            throw new ReglaNegocioException("RN-A2", "Ya existe un precio de referencia con la clave " + solicitud.clave() + ".");
        }
        PrecioReferencia precio = new PrecioReferencia();
        aplicar(precio, solicitud);
        return PrecioReferenciaResponse.de(precios.save(precio));
    }

    @Transactional
    public PrecioReferenciaResponse actualizar(Long id, PrecioReferenciaRequest solicitud) {
        PrecioReferencia precio = precios.findById(id).orElseThrow(() -> new NoEncontradoException("Precio de referencia", id));
        if (!precio.getClave().equals(solicitud.clave())) {
            throw new ReglaNegocioException("RN-A2", "La clave de un precio de referencia no se cambia.");
        }
        aplicar(precio, solicitud);
        return PrecioReferenciaResponse.de(precio);
    }

    private static void aplicar(PrecioReferencia precio, PrecioReferenciaRequest s) {
        if (s.precioMinoristaKg().compareTo(s.precioMayoristaKg()) < 0) {
            throw new ReglaNegocioException("RN-A2", "El precio minorista no puede ser menor que el mayorista.");
        }
        precio.setClave(s.clave());
        precio.setNombre(s.nombre().trim());
        precio.setPrecioMayoristaKg(Dinero.redondear(s.precioMayoristaKg()));
        precio.setPrecioMinoristaKg(Dinero.redondear(s.precioMinoristaKg()));
        precio.setFuente(s.fuente().trim());
        precio.setFecha(s.fecha());
    }
}
