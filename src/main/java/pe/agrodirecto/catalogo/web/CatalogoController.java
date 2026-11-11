package pe.agrodirecto.catalogo.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.catalogo.servicio.CatalogoService;

/** Catálogo público: cualquier visitante puede explorar sin registrarse. */
@RestController
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService servicio;

    @GetMapping("/api/productos")
    public List<ProductoResumenResponse> buscar(@RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria, @RequestParam(required = false) String region,
            @RequestParam(required = false) String atiende, @RequestParam(required = false) Boolean organico,
            @RequestParam(required = false) Long productorId, @RequestParam(required = false) String orden) {
        return servicio.buscar(new CatalogoService.Filtro(q, categoria, region, atiende, organico, productorId, orden));
    }

    @GetMapping("/api/productos/{id}")
    public ProductoDetalleResponse detalle(@PathVariable Long id) {
        return servicio.detalle(id);
    }

    @GetMapping("/api/categorias")
    public List<CategoriaResponse> categorias() {
        return servicio.categorias();
    }

    @GetMapping("/api/precios-referencia")
    public List<PrecioReferenciaResponse> preciosReferencia() {
        return servicio.preciosReferencia();
    }
}
