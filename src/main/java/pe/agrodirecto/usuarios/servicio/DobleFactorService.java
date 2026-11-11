package pe.agrodirecto.usuarios.servicio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.compartido.error.AccesoDenegadoException;
import pe.agrodirecto.compartido.error.DemasiadosIntentosException;
import pe.agrodirecto.compartido.error.NoAutenticadoException;
import pe.agrodirecto.compartido.error.ReglaNegocioException;
import pe.agrodirecto.config.AgroDirectoProperties;
import pe.agrodirecto.usuarios.dominio.CodigoRespaldo;
import pe.agrodirecto.usuarios.dominio.Usuario;
import pe.agrodirecto.usuarios.repositorio.CodigoRespaldoRepository;
import pe.agrodirecto.usuarios.repositorio.UsuarioRepository;
import pe.agrodirecto.usuarios.seguridad.CifradoSecretos;
import pe.agrodirecto.usuarios.seguridad.Totp;
import pe.agrodirecto.usuarios.servicio.TokenService.TokenEmitido;
import pe.agrodirecto.usuarios.web.DobleFactorDtos.ActivacionResponse;
import pe.agrodirecto.usuarios.web.DobleFactorDtos.ConfiguracionResponse;
import pe.agrodirecto.usuarios.web.DobleFactorDtos.EstadoResponse;
import pe.agrodirecto.usuarios.web.SesionResponse;
import pe.agrodirecto.usuarios.web.UsuarioResponse;

/**
 * Verificación en dos pasos con TOTP: algo que sabes (contraseña) + algo que tienes (el celular con la app).
 * Obligatoria para los roles con más poder (administración y agentes, que verifican agricultores y mueven pedidos).
 */
@Service
@RequiredArgsConstructor
public class DobleFactorService {

    public static final String EMISOR = "AgroDirecto";
    private static final int CODIGOS_RESPALDO = 8;
    /** Sin caracteres que se confunden al copiarlos a mano (0/O, 1/I/L). */
    private static final String ALFABETO_RESPALDO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final CodigoRespaldoRepository respaldos;
    private final UsuarioActualService usuarioActual;
    private final TokenService tokenService;
    private final CifradoSecretos cifrado;
    private final AgroDirectoProperties propiedades;
    private final Clock reloj;

    @Transactional(readOnly = true)
    public EstadoResponse estado(Jwt jwt) {
        Usuario usuario = usuarioActual.obtenerSinSegundoFactor(jwt);
        return new EstadoResponse(usuario.isTotpActivo(), obligatorio(usuario),
                respaldos.countByUsuarioIdAndUsadoFalse(usuario.getId()));
    }

    /** Paso 1: genera el secreto (aún inactivo) para escanearlo con la app. */
    @Transactional
    public ConfiguracionResponse configurar(Jwt jwt) {
        Usuario usuario = usuarioActual.obtenerSinSegundoFactor(jwt);
        if (usuario.isTotpActivo()) {
            throw new ReglaNegocioException("SEG-2FA", "La verificación en dos pasos ya está activa.");
        }
        String secreto = Totp.nuevoSecreto();
        usuario.setTotpSecreto(cifrado.cifrar(secreto));
        return new ConfiguracionResponse(secreto, Totp.uri(EMISOR, usuario.getEmail(), secreto),
                "Escanea el código QR con Google Authenticator, Microsoft Authenticator o Authy "
                        + "y envía el código de 6 dígitos que aparece para activarla.");
    }

    /** Paso 2: el usuario demuestra que su app genera códigos correctos. Recibe sus códigos de respaldo. */
    @Transactional
    public ActivacionResponse activar(Jwt jwt, String codigo) {
        Usuario usuario = usuarioActual.obtenerSinSegundoFactor(jwt);
        if (usuario.isTotpActivo()) {
            throw new ReglaNegocioException("SEG-2FA", "La verificación en dos pasos ya está activa.");
        }
        if (usuario.getTotpSecreto() == null) {
            throw new ReglaNegocioException("SEG-2FA", "Primero solicita la configuración en /api/auth/2fa/configurar.");
        }
        OptionalLong paso = Totp.verificar(cifrado.descifrar(usuario.getTotpSecreto()), codigo, reloj.instant());
        if (paso.isEmpty()) {
            throw new ReglaNegocioException("SEG-2FA",
                    "El código no coincide. Revisa que la hora de tu celular sea automática.");
        }
        usuario.setTotpActivo(true);
        usuario.setTotpUltimoPaso(paso.getAsLong());
        List<String> codigos = regenerarRespaldos(usuario.getId());
        TokenEmitido token = tokenService.emitir(usuario, true);
        return new ActivacionResponse(codigos,
                SesionResponse.completa(token.token(), token.expiraEn(), UsuarioResponse.de(usuario)));
    }

    /**
     * Paso final del login: token temporal + código de la app (o un código de respaldo).
     * Cada código vale una sola vez y los fallos cuentan para el bloqueo temporal de la cuenta.
     */
    @Transactional(noRollbackFor = NoAutenticadoException.class)
    public SesionResponse verificarInicioSesion(String tokenTemporal, String codigo) {
        Long id = tokenService.validarTemporal(tokenTemporal);
        Usuario usuario = usuarios.findById(id).orElseThrow(() -> new NoAutenticadoException("La cuenta no existe."));
        LocalDateTime ahora = LocalDateTime.now(reloj);
        AgroDirectoProperties.Seguridad seguridad = propiedades.seguridad();
        if (usuario.estaBloqueado(ahora)) {
            throw new DemasiadosIntentosException("Demasiados intentos fallidos. Intenta de nuevo en "
                    + seguridad.minutosBloqueo() + " minutos.");
        }
        if (!usuario.estaActivo()) {
            throw new AccesoDenegadoException("RN-23", "Tu cuenta está suspendida. Comunícate con AgroDirecto.");
        }
        if (!usuario.isTotpActivo() || !codigoValido(usuario, codigo.trim(), ahora)) {
            usuario.registrarIntentoFallido(ahora, seguridad.intentosMaximos(), Duration.ofMinutes(seguridad.minutosBloqueo()));
            throw new NoAutenticadoException("Código incorrecto o ya utilizado.");
        }
        usuario.registrarAccesoExitoso(ahora);
        TokenEmitido token = tokenService.emitir(usuario, true);
        return SesionResponse.completa(token.token(), token.expiraEn(), UsuarioResponse.de(usuario));
    }

    /** Solo para roles donde es opcional, y confirmando con un código vigente. */
    @Transactional
    public EstadoResponse desactivar(Jwt jwt, String codigo) {
        Usuario usuario = usuarioActual.obtener(jwt);
        if (obligatorio(usuario)) {
            throw new ReglaNegocioException("SEG-2FA", "Tu rol requiere la verificación en dos pasos.");
        }
        if (!usuario.isTotpActivo() || !codigoValido(usuario, codigo.trim(), LocalDateTime.now(reloj))) {
            throw new ReglaNegocioException("SEG-2FA", "Código incorrecto.");
        }
        usuario.setTotpActivo(false);
        usuario.setTotpSecreto(null);
        usuario.setTotpUltimoPaso(null);
        respaldos.deleteAll(respaldos.findByUsuarioId(usuario.getId()));
        return new EstadoResponse(false, false, 0);
    }

    // ---------- Internos ----------

    private boolean obligatorio(Usuario usuario) {
        return propiedades.seguridad().rolesConDobleFactor().contains(usuario.getRol());
    }

    private boolean codigoValido(Usuario usuario, String codigo, LocalDateTime ahora) {
        if (codigo.matches("\\d{" + Totp.DIGITOS + "}")) {
            OptionalLong paso = Totp.verificar(cifrado.descifrar(usuario.getTotpSecreto()), codigo, reloj.instant());
            // Protección contra repetición: un código interceptado no se puede volver a usar.
            if (paso.isEmpty() || (usuario.getTotpUltimoPaso() != null && paso.getAsLong() <= usuario.getTotpUltimoPaso())) {
                return false;
            }
            usuario.setTotpUltimoPaso(paso.getAsLong());
            return true;
        }
        Optional<CodigoRespaldo> respaldo = respaldos.findByUsuarioIdAndHashAndUsadoFalse(usuario.getId(),
                hash(normalizar(codigo)));
        respaldo.ifPresent(r -> {
            r.setUsado(true);
            r.setUsadoEn(ahora);
        });
        return respaldo.isPresent();
    }

    private List<String> regenerarRespaldos(Long usuarioId) {
        respaldos.deleteAll(respaldos.findByUsuarioId(usuarioId));
        List<String> codigos = new ArrayList<>();
        for (int i = 0; i < CODIGOS_RESPALDO; i++) {
            StringBuilder codigo = new StringBuilder();
            for (int j = 0; j < 10; j++) {
                codigo.append(ALFABETO_RESPALDO.charAt(ALEATORIO.nextInt(ALFABETO_RESPALDO.length())));
            }
            String visible = codigo.substring(0, 5) + "-" + codigo.substring(5);
            CodigoRespaldo respaldo = new CodigoRespaldo();
            respaldo.setUsuarioId(usuarioId);
            respaldo.setHash(hash(normalizar(visible)));
            respaldos.save(respaldo);
            codigos.add(visible);
        }
        return codigos;
    }

    private static String normalizar(String codigo) {
        return codigo.replace("-", "").replace(" ", "").toUpperCase();
    }

    private static String hash(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
