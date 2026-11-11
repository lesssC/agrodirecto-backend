package pe.agrodirecto.catalogo.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.catalogo.dominio.EstadoProducto;
import pe.agrodirecto.catalogo.dominio.FormatoVenta;
import pe.agrodirecto.catalogo.dominio.Producto;
import pe.agrodirecto.catalogo.repositorio.CategoriaRepository;
import pe.agrodirecto.catalogo.repositorio.PrecioReferenciaRepository;
import pe.agrodirecto.catalogo.repositorio.ProductoRepository;
import pe.agrodirecto.catalogo.web.FormatoRequest;
import pe.agrodirecto.catalogo.web.ProductoGestionResponse;
import pe.agrodirecto.catalogo.web.ProductoRequest;
import pe.agrodirecto.compartido.Dinero;
import pe.agrodirecto.compartido.Texto;
import pe.agrodirecto.compartido.error.NoEncontradoException;
import pe.agrodirecto.compartido.error.ReglaNegocioException;
import pe.agrodirecto.productores.dominio.Productor;
import pe.agrodirecto.productores.servicio.ProductorService;
import pe.agrodirecto.productores.web.ProductorGestionResponse;

/** Publicación y mantenimiento de productos por el agricultor, su agente o la administración. */
@Service
@RequiredArgsConstructor
public class GestionProductoService {

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final PrecioReferenciaRepository preciosReferencia;
    private final ProductorService productorService;
    private final AlertaPrecioService alertas;
    private final Clock reloj;

    /** Productos de un productor concreto o, sin filtro, de todos los que el usuario gestiona. */
    @Transactional(readOnly = true)
    public List<ProductoGestionResponse> listar(Jwt jwt, Long productorId) {
        List<Long> ids = productorId != null
                ? List.of(productorService.buscarGestionable(jwt, productorId).getId())
                : productorService.gestionados(jwt).stream().map(ProductorGestionResponse::id).toList();
        return productos.findByProductorIdInOrderByActualizadoEnDesc(ids).stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public ProductoGestionResponse detalle(Jwt jwt, Long id) {
        return respuesta(buscarGestionable(jwt, id));
    }

    @Transactional
    public ProductoGestionResponse crear(Jwt jwt, ProductoRequest solicitud) {
        Productor productor = productorService.buscarGestionable(jwt, solicitud.productorId());
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Producto producto = new Producto();
        producto.setProductor(productor);
        producto.setCreadoEn(ahora);
        aplicar(producto, solicitud, ahora);
        return respuesta(productos.save(producto));
    }

    @Transactional
    public ProductoGestionResponse actualizar(Jwt jwt, Long id, ProductoRequest solicitud) {
        Producto producto = buscarGestionable(jwt, id);
        if (!producto.getProductor().getId().equals(solicitud.productorId())) {
            throw new ReglaNegocioException(null, "Un producto no puede pasar a otro productor.");
        }
        aplicar(producto, solicitud, LocalDateTime.now(reloj));
        productos.flush();
        return respuesta(producto);
    }

    /** Publicar, volver a borrador o retirar. Un producto retirado se puede republicar. */
    @Transactional
    public ProductoGestionResponse cambiarEstado(Jwt jwt, Long id, EstadoProducto estado) {
        Producto producto = buscarGestionable(jwt, id);
        if (estado == EstadoProducto.ACTIVO) {
            validarPublicacion(producto);
        }
        producto.setEstado(estado);
        producto.setActualizadoEn(LocalDateTime.now(reloj));
        return respuesta(producto);
    }

    // ---------- Internos ----------

    private Producto buscarGestionable(Jwt jwt, Long id) {
        Producto producto = productos.findById(id).orElseThrow(() -> new NoEncontradoException("Producto", id));
        productorService.buscarGestionable(jwt, producto.getProductor().getId());
        return producto;
    }

    private void aplicar(Producto producto, ProductoRequest s, LocalDateTime ahora) {
        producto.setCategoria(categorias.findById(s.categoriaId())
                .orElseThrow(() -> new NoEncontradoException("Categoría", s.categoriaId())));
        producto.setPrecioReferencia(s.precioReferenciaId() == null ? null
                : preciosReferencia.findById(s.precioReferenciaId())
                        .orElseThrow(() -> new NoEncontradoException("Precio de referencia", s.precioReferenciaId())));
        producto.setNombre(s.nombre().trim());
        producto.setDescripcion(s.descripcion());
        producto.setImagenUrl(s.imagenUrl());
        producto.setOrganico(s.organico());
        producto.setFechaCosecha(s.fechaCosecha());
        producto.setVidaUtilDias(s.vidaUtilDias());
        producto.setCalibre(s.calibre());
        aplicarFormatos(producto, s.formatos());
        producto.setActualizadoEn(ahora);

        if (s.publicar()) { // nunca es null: el record lo normaliza
            validarPublicacion(producto);
            producto.setEstado(EstadoProducto.ACTIVO);
        } else if (producto.getEstado() != EstadoProducto.RETIRADO) {
            producto.setEstado(EstadoProducto.BORRADOR);
        }
    }

    /**
     * Los formatos enviados con id se actualizan; los nuevos se agregan; los omitidos se desactivan.
     * No se borran porque los pedidos anteriores los siguen referenciando.
     */
    private void aplicarFormatos(Producto producto, List<FormatoRequest> solicitud) {
        Set<String> nombres = new HashSet<>();
        Set<Long> enviados = new HashSet<>();
        for (FormatoRequest f : solicitud) {
            if (!nombres.add(Texto.normalizar(f.nombre()))) {
                throw new ReglaNegocioException("RN-02", "La presentación «" + f.nombre() + "» está repetida.");
            }
            if ((f.cantidadMinDescuento() == null) != (f.porcentajeDescuento() == null)) {
                throw new ReglaNegocioException("RN-A1",
                        "El descuento por volumen necesita la cantidad mínima y el porcentaje.");
            }
            FormatoVenta formato = f.id() == null ? nuevoFormato(producto) : formatoExistente(producto, f.id());
            enviados.add(f.id());
            formato.setNombre(f.nombre().trim());
            formato.setTipo(f.tipo());
            formato.setPesoKg(f.pesoKg());
            formato.setPrecio(Dinero.redondear(f.precio()));
            formato.setStock(f.stock());
            formato.setCantidadMinDescuento(f.cantidadMinDescuento());
            formato.setPorcentajeDescuento(f.porcentajeDescuento());
            formato.setActivo(true);
        }
        producto.getFormatos().stream()
                .filter(f -> f.getId() != null && !enviados.contains(f.getId()))
                .forEach(f -> f.setActivo(false));
    }

    private static FormatoVenta nuevoFormato(Producto producto) {
        FormatoVenta formato = new FormatoVenta();
        producto.agregarFormato(formato);
        return formato;
    }

    private static FormatoVenta formatoExistente(Producto producto, Long id) {
        return producto.getFormatos().stream().filter(f -> Objects.equals(f.getId(), id)).findFirst()
                .orElseThrow(() -> new ReglaNegocioException("RN-02",
                        "La presentación " + id + " no pertenece a este producto."));
    }

    /**
     * Para publicar, el agricultor debe estar verificado en persona (RN-B5) y activo, y el producto debe tener
     * al menos una presentación a la venta. Un agricultor sin verificar puede preparar borradores.
     */
    private static void validarPublicacion(Producto producto) {
        Productor productor = producto.getProductor();
        if (!productor.estaActivo()) {
            throw new ReglaNegocioException("RN-23", "El productor está suspendido y no puede publicar.");
        }
        if (!productor.isVerificado()) {
            throw new ReglaNegocioException("RN-B5", productor.getNombreComercial()
                    + " aún no ha sido verificado en persona. Guarda el producto como borrador hasta su verificación.");
        }
        if (producto.formatosActivos().isEmpty()) {
            throw new ReglaNegocioException("RN-02", "Agrega al menos una presentación de venta antes de publicar.");
        }
    }

    private ProductoGestionResponse respuesta(Producto producto) {
        return ProductoGestionResponse.de(producto, alertas.evaluar(producto));
    }
}
