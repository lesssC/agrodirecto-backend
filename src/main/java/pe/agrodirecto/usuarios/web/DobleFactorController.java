package pe.agrodirecto.usuarios.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.agrodirecto.usuarios.servicio.DobleFactorService;
import pe.agrodirecto.usuarios.web.DobleFactorDtos.ActivacionResponse;
import pe.agrodirecto.usuarios.web.DobleFactorDtos.CodigoRequest;
import pe.agrodirecto.usuarios.web.DobleFactorDtos.ConfiguracionResponse;
import pe.agrodirecto.usuarios.web.DobleFactorDtos.EstadoResponse;
import pe.agrodirecto.usuarios.web.DobleFactorDtos.VerificacionRequest;

@RestController
@RequestMapping("/api/auth/2fa")
@RequiredArgsConstructor
public class DobleFactorController {

    private final DobleFactorService servicio;

    @GetMapping
    public EstadoResponse estado(@AuthenticationPrincipal Jwt jwt) {
        return servicio.estado(jwt);
    }

    @PostMapping("/configurar")
    public ConfiguracionResponse configurar(@AuthenticationPrincipal Jwt jwt) {
        return servicio.configurar(jwt);
    }

    @PostMapping("/activar")
    public ActivacionResponse activar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CodigoRequest solicitud) {
        return servicio.activar(jwt, solicitud.codigo());
    }

    /** Segundo paso del login. Es público: se autentica con el token temporal del cuerpo. */
    @PostMapping("/verificar")
    public SesionResponse verificar(@Valid @RequestBody VerificacionRequest solicitud) {
        return servicio.verificarInicioSesion(solicitud.tokenTemporal(), solicitud.codigo());
    }

    @PostMapping("/desactivar")
    public EstadoResponse desactivar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CodigoRequest solicitud) {
        return servicio.desactivar(jwt, solicitud.codigo());
    }
}
