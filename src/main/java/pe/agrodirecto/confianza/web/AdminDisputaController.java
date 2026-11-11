package pe.agrodirecto.confianza.web;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.agrodirecto.confianza.dominio.EstadoDisputa;
import pe.agrodirecto.confianza.servicio.DisputaService;

/** Mediación de la administración. La ruta /api/admin/** ya exige el rol ADMINISTRADOR. */
@RestController
@RequestMapping("/api/admin/disputas")
@RequiredArgsConstructor
public class AdminDisputaController {

    private final DisputaService disputas;

    @GetMapping
    public List<DisputaResponse> listar(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) EstadoDisputa estado) {
        return disputas.listar(jwt, estado);
    }

    @PostMapping("/{id}/resolver")
    public DisputaResponse resolver(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody ResolucionRequest solicitud) {
        return disputas.resolver(jwt, id, solicitud);
    }
}
