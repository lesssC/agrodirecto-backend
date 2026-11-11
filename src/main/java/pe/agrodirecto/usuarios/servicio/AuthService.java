package pe.agrodirecto.usuarios.servicio;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.compartido.error.AccesoDenegadoException;
import pe.agrodirecto.compartido.error.DemasiadosIntentosException;
import pe.agrodirecto.compartido.error.NoAutenticadoException;
import pe.agrodirecto.compartido.error.ReglaNegocioException;
import pe.agrodirecto.config.AgroDirectoProperties;
import pe.agrodirecto.usuarios.dominio.Rol;
import pe.agrodirecto.usuarios.dominio.Usuario;
import pe.agrodirecto.usuarios.repositorio.UsuarioRepository;
import pe.agrodirecto.usuarios.servicio.TokenService.TokenEmitido;
import pe.agrodirecto.usuarios.web.LoginRequest;
import pe.agrodirecto.usuarios.web.RegistroRequest;
import pe.agrodirecto.usuarios.web.SesionResponse;
import pe.agrodirecto.usuarios.web.UsuarioResponse;

@Service
@RequiredArgsConstructor
public class AuthService {

    static final String CREDENCIALES_INVALIDAS = "Correo o contraseña incorrectos.";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AgroDirectoProperties propiedades;
    private final Clock reloj;

    @Transactional
    public UsuarioResponse registrar(RegistroRequest solicitud) {
        if (!solicitud.rol().permiteAutoregistro()) {
            throw new ReglaNegocioException("RN-07", "Las cuentas de administrador no se crean por registro público.");
        }
        if (solicitud.rol() == Rol.AGENTE && solicitud.dni() == null) {
            throw new ReglaNegocioException("RN-17", "El agente de campo debe registrar su DNI para poder ser verificado.");
        }
        String email = solicitud.email().trim().toLowerCase(Locale.ROOT);
        // RN-23: una persona, una cuenta. Se evita que un sancionado vuelva con otra cuenta.
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new ReglaNegocioException("RN-23", "Ya existe una cuenta con ese correo.");
        }
        if (usuarios.existsByTelefono(solicitud.telefono())) {
            throw new ReglaNegocioException("RN-23", "Ya existe una cuenta con ese número de celular.");
        }
        if (solicitud.dni() != null && usuarios.existsByDni(solicitud.dni())) {
            throw new ReglaNegocioException("RN-23", "Ya existe una cuenta con ese DNI.");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(solicitud.nombre().trim());
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(solicitud.password()));
        usuario.setTelefono(solicitud.telefono());
        usuario.setDni(solicitud.dni());
        usuario.setRuc(solicitud.ruc());
        usuario.setRazonSocial(solicitud.razonSocial());
        usuario.setRol(solicitud.rol());
        usuario.setVerificado(false);
        usuario.setCreadoEn(ahora());
        return UsuarioResponse.de(usuarios.save(usuario));
    }

    /** El intento fallido se guarda aunque se lance la excepción (noRollbackFor). */
    @Transactional(noRollbackFor = NoAutenticadoException.class)
    public SesionResponse iniciarSesion(LoginRequest solicitud) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(solicitud.email().trim())
                .orElseThrow(() -> new NoAutenticadoException(CREDENCIALES_INVALIDAS));
        LocalDateTime ahora = ahora();
        AgroDirectoProperties.Seguridad seguridad = propiedades.seguridad();

        if (usuario.estaBloqueado(ahora)) {
            throw new DemasiadosIntentosException("Demasiados intentos fallidos. Intenta de nuevo en "
                    + seguridad.minutosBloqueo() + " minutos.");
        }
        if (!passwordEncoder.matches(solicitud.password(), usuario.getPasswordHash())) {
            usuario.registrarIntentoFallido(ahora, seguridad.intentosMaximos(),
                    Duration.ofMinutes(seguridad.minutosBloqueo()));
            throw new NoAutenticadoException(CREDENCIALES_INVALIDAS);
        }
        if (!usuario.estaActivo()) {
            throw new AccesoDenegadoException("RN-23", "Tu cuenta está suspendida. Comunícate con AgroDirecto.");
        }
        if (usuario.isTotpActivo()) {
            // La contraseña es correcta, pero falta el código. Los intentos fallidos no se reinician aquí:
            // así no se puede adivinar el código volviendo a ingresar la contraseña una y otra vez.
            TokenEmitido temporal = tokenService.emitirTemporal(usuario);
            return SesionResponse.pendienteDeCodigo(temporal.token(), temporal.expiraEn());
        }

        usuario.registrarAccesoExitoso(ahora);
        TokenEmitido token = tokenService.emitir(usuario, false);
        return SesionResponse.completa(token.token(), token.expiraEn(), UsuarioResponse.de(usuario));
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }
}
