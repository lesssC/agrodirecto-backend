package pe.agrodirecto.usuarios.servicio;

import java.util.Arrays;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.compartido.error.AccesoDenegadoException;
import pe.agrodirecto.config.AgroDirectoProperties;
import pe.agrodirecto.compartido.error.NoAutenticadoException;
import pe.agrodirecto.usuarios.dominio.Rol;
import pe.agrodirecto.usuarios.dominio.Usuario;
import pe.agrodirecto.usuarios.repositorio.UsuarioRepository;

/**
 * Obtiene el usuario dueño del JWT. Otros módulos lo usan para conocer quién opera.
 * Revisa el estado en la base: una cuenta suspendida queda bloqueada aunque su token siga vigente.
 */
@Service
@RequiredArgsConstructor
public class UsuarioActualService {

    private final UsuarioRepository usuarios;
    private final AgroDirectoProperties propiedades;

    /**
     * Usuario del token, exigiendo además el segundo factor cuando corresponde:
     * <ul>
     * <li>si la cuenta tiene TOTP activo, el token debe haberse emitido tras verificar el código;</li>
     * <li>si su rol lo exige (administrador, agente) y aún no lo configuró, solo puede configurarlo.</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public Usuario obtener(Jwt jwt) {
        Usuario usuario = obtenerSinSegundoFactor(jwt);
        if (usuario.isTotpActivo() && !TokenService.usoSegundoFactor(jwt)) {
            throw new NoAutenticadoException("Tu cuenta usa verificación en dos pasos. Vuelve a iniciar sesión con tu código.");
        }
        if (!usuario.isTotpActivo() && propiedades.seguridad().rolesConDobleFactor().contains(usuario.getRol())) {
            throw new AccesoDenegadoException("SEG-2FA", "Tu rol requiere verificación en dos pasos. "
                    + "Configúrala en /api/auth/2fa/configurar antes de continuar.");
        }
        return usuario;
    }

    /** Solo identidad y estado de la cuenta; para configurar el segundo factor o consultar el propio perfil. */
    @Transactional(readOnly = true)
    public Usuario obtenerSinSegundoFactor(Jwt jwt) {
        if (jwt == null) {
            throw new NoAutenticadoException("Debes iniciar sesión.");
        }
        Usuario usuario = usuarios.findById(Long.valueOf(jwt.getSubject()))
                .orElseThrow(() -> new NoAutenticadoException("La cuenta del token ya no existe."));
        if (!usuario.estaActivo()) {
            throw new AccesoDenegadoException("RN-23", "Tu cuenta está suspendida. Comunícate con AgroDirecto.");
        }
        return usuario;
    }

    /** RN-07: las funciones se habilitan según el rol de la sesión. */
    public Usuario exigirRol(Jwt jwt, Rol... permitidos) {
        Usuario usuario = obtener(jwt);
        if (Arrays.stream(permitidos).noneMatch(rol -> rol == usuario.getRol())) {
            throw new AccesoDenegadoException("Esta operación no está disponible para el rol " + usuario.getRol() + ".");
        }
        return usuario;
    }
}
