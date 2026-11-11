package pe.agrodirecto.productores.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.compartido.Regiones;
import pe.agrodirecto.productores.servicio.ProductorService;

/** Directorio público de agricultores: cualquiera puede conocer quién produce y desde dónde. */
@RestController
@RequiredArgsConstructor
public class ProductorPublicoController {

    private final ProductorService servicio;

    /**
     * @param region  región de origen del agricultor
     * @param atiende región a la que el comprador necesita que le entreguen
     */
    @GetMapping("/api/productores")
    public List<ProductorPublicoResponse> directorio(@RequestParam(required = false) String region,
            @RequestParam(required = false) String atiende) {
        return servicio.directorio(region, atiende);
    }

    @GetMapping("/api/productores/{id}")
    public ProductorPublicoResponse detalle(@PathVariable Long id) {
        return servicio.detallePublico(id);
    }

    @GetMapping("/api/asociaciones")
    public List<AsociacionResponse> asociaciones() {
        return servicio.asociaciones();
    }

    @GetMapping("/api/regiones")
    public List<String> regiones() {
        return Regiones.TODAS;
    }
}
