package pe.agrodirecto.administracion.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.agrodirecto.administracion.servicio.AdministracionService;
import pe.agrodirecto.administracion.servicio.MetricasService;
import pe.agrodirecto.catalogo.web.PrecioReferenciaRequest;
import pe.agrodirecto.catalogo.web.PrecioReferenciaResponse;
import pe.agrodirecto.productores.web.ProductorGestionResponse;
import pe.agrodirecto.usuarios.dominio.EstadoCuenta;
import pe.agrodirecto.usuarios.dominio.Rol;
import pe.agrodirecto.usuarios.web.UsuarioResponse;

/** Panel de administración. Spring Security ya exige el rol ADMINISTRADOR en /api/admin/**. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MetricasService metricas;
    private final AdministracionService servicio;

    @GetMapping("/resumen")
    public ResumenResponse resumen(@AuthenticationPrincipal Jwt jwt) {
        servicio.exigirAdministrador(jwt);
        return metricas.resumen();
    }

    // ---------- Usuarios ----------

    @GetMapping("/usuarios")
    public List<UsuarioResponse> usuarios(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Rol rol,
            @RequestParam(required = false) Boolean verificado, @RequestParam(required = false) EstadoCuenta estado) {
        return servicio.usuarios(jwt, rol, verificado, estado);
    }

    @PostMapping("/usuarios/{id}/verificar")
    public UsuarioResponse verificar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody MotivoAdminRequest solicitud) {
        return servicio.verificar(jwt, id, solicitud.motivo());
    }

    @PostMapping("/usuarios/{id}/suspender")
    public UsuarioResponse suspender(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody MotivoAdminRequest solicitud) {
        return servicio.suspender(jwt, id, solicitud.motivo());
    }

    @PostMapping("/usuarios/{id}/reactivar")
    public UsuarioResponse reactivar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody MotivoAdminRequest solicitud) {
        return servicio.reactivar(jwt, id, solicitud.motivo());
    }

    // ---------- Productores ----------

    @PostMapping("/productores/{id}/suspender")
    public ProductorGestionResponse suspenderProductor(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody MotivoAdminRequest solicitud) {
        return servicio.suspenderProductor(jwt, id, solicitud.motivo());
    }

    @PostMapping("/productores/{id}/reactivar")
    public ProductorGestionResponse reactivarProductor(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody MotivoAdminRequest solicitud) {
        return servicio.reactivarProductor(jwt, id, solicitud.motivo());
    }

    @PostMapping("/productores/{id}/agente")
    public ProductorGestionResponse reasignarAgente(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody ReasignarAgenteRequest solicitud) {
        return servicio.reasignarAgente(jwt, id, solicitud.agenteId(), solicitud.motivo());
    }

    // ---------- Precios de referencia ----------

    @PostMapping("/precios-referencia")
    @ResponseStatus(HttpStatus.CREATED)
    public PrecioReferenciaResponse crearPrecio(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PrecioReferenciaRequest solicitud) {
        return servicio.crearPrecio(jwt, solicitud);
    }

    @PutMapping("/precios-referencia/{id}")
    public PrecioReferenciaResponse actualizarPrecio(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody PrecioReferenciaRequest solicitud) {
        return servicio.actualizarPrecio(jwt, id, solicitud);
    }

    // ---------- Auditoría ----------

    @GetMapping("/auditoria")
    public List<AccionResponse> auditoria(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String objetivo, @RequestParam(required = false) Long objetivoId) {
        return servicio.auditoria(jwt, objetivo, objetivoId);
    }
}
