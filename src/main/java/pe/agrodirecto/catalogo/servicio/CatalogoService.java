package pe.agrodirecto.catalogo.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.catalogo.dominio.EstadoProducto;
import pe.agrodirecto.catalogo.dominio.Producto;
import pe.agrodirecto.catalogo.repositorio.CategoriaRepository;
import pe.agrodirecto.catalogo.repositorio.PrecioReferenciaRepository;
import pe.agrodirecto.catalogo.repositorio.ProductoRepository;
import pe.agrodirecto.catalogo.web.CategoriaResponse;
import pe.agrodirecto.catalogo.web.PrecioReferenciaResponse;
import pe.agrodirecto.catalogo.web.ProductoDetalleResponse;
import pe.agrodirecto.catalogo.web.ProductoResumenResponse;
import pe.agrodirecto.compartido.Regiones;
import pe.agrodirecto.compartido.Texto;
import pe.agrodirecto.compartido.error.NoEncontradoException;

/** Consultas públicas del catálogo. */
@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final PrecioReferenciaRepository preciosReferencia;

    /**
     * @param q         texto libre: nombre del producto, del productor, provincia o región (ignora tildes)
     * @param categoria nombre de la categoría (ignora tildes)
     * @param region    región de origen del productor
     * @param atiende   región de entrega: solo productores que la cubren (RN-09)
     * @param orden     {@code precio_asc}, {@code precio_desc}, {@code cosecha} o vacío (orden del catálogo)
     */
    public record Filtro(String q, String categoria, String region, String atiende, Boolean organico,
            Long productorId, String orden) {
    }

    @Transactional(readOnly = true)
    public List<ProductoResumenResponse> buscar(Filtro filtro) {
        // Una región que no existe no puede tener productos.
        if (regionInexistente(filtro.region()) || regionInexistente(filtro.atiende())) {
            return List.of();
        }
        String region = regionOpcional(filtro.region());
        String atiende = regionOpcional(filtro.atiende());
        Stream<Producto> resultado = productos.buscarPublicados(EstadoProducto.ACTIVO, filtro.productorId(), region,
                atiende, filtro.organico()).stream();

        if (filtro.categoria() != null && !filtro.categoria().isBlank()) {
            String categoria = Texto.normalizar(filtro.categoria());
            resultado = resultado.filter(p -> Texto.normalizar(p.getCategoria().getNombre()).equals(categoria));
        }
        if (filtro.q() != null && !filtro.q().isBlank()) {
            String termino = Texto.normalizar(filtro.q());
            resultado = resultado.filter(p -> Texto.normalizar(String.join(" ", p.getNombre(),
                    p.getProductor().getNombreComercial(), p.getProductor().getProvincia(),
                    p.getProductor().getRegion())).contains(termino));
        }
        return ordenar(resultado, filtro.orden()).map(ProductoResumenResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ProductoDetalleResponse detalle(Long id) {
        Producto producto = productos.findById(id)
                .filter(Producto::visibleEnCatalogo)
                .orElseThrow(() -> new NoEncontradoException("Producto", id));
        return ProductoDetalleResponse.de(producto);
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> categorias() {
        return categorias.findAllByOrderByNombre().stream().map(CategoriaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<PrecioReferenciaResponse> preciosReferencia() {
        return preciosReferencia.findAllByOrderByNombre().stream().map(PrecioReferenciaResponse::de).toList();
    }

    private static Stream<Producto> ordenar(Stream<Producto> productos, String orden) {
        Comparator<Producto> porPrecio = Comparator.comparing(p -> p.precioDesde().orElse(BigDecimal.ZERO));
        if (orden == null) {
            return productos;
        }
        return switch (orden) {
            case "precio_asc" -> productos.sorted(porPrecio);
            case "precio_desc" -> productos.sorted(porPrecio.reversed());
            case "cosecha" -> productos.sorted(Comparator.comparing(Producto::getFechaCosecha,
                    Comparator.nullsLast(Comparator.<LocalDate>reverseOrder())));
            default -> productos;
        };
    }

    private static boolean regionInexistente(String region) {
        return region != null && !region.isBlank() && Regiones.normalizar(region).isEmpty();
    }

    private static String regionOpcional(String region) {
        return region == null || region.isBlank() ? null : Regiones.normalizar(region).orElse(null);
    }
}
