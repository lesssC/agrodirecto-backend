package pe.agrodirecto.usuarios.servicio;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import pe.agrodirecto.compartido.error.NoAutenticadoException;
import pe.agrodirecto.config.AgroDirectoProperties;
import pe.agrodirecto.config.SeguridadConfig;
import pe.agrodirecto.usuarios.dominio.Usuario;

/**
 * Emite dos clases de JWT:
 * <ul>
 * <li><b>Acceso</b>: {@code sub} = id, {@code rol}, y {@code amr} con los factores usados ("pwd" y, si aplica, "otp").</li>
 * <li><b>Temporal</b>: 5 minutos, con {@code etapa = SEGUNDO_FACTOR}; solo sirve para enviar el código TOTP.
 * La API lo rechaza (ver {@link SeguridadConfig}).</li>
 * </ul>
 */
@Service
public class TokenService {

    public static final String EMISOR = "agrodirecto";
    public static final String CLAIM_FACTORES = "amr";
    public static final String FACTOR_CONTRASENA = "pwd";
    public static final String FACTOR_OTP = "otp";
    private static final int MINUTOS_TOKEN_TEMPORAL = 5;

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder decodificadorTemporal;
    private final AgroDirectoProperties propiedades;
    private final Clock reloj;

    public TokenService(JwtEncoder jwtEncoder, SecretKey claveJwt, AgroDirectoProperties propiedades, Clock reloj) {
        this.jwtEncoder = jwtEncoder;
        // Decodificador propio, sin el validador que rechaza tokens temporales.
        this.decodificadorTemporal = NimbusJwtDecoder.withSecretKey(claveJwt).macAlgorithm(MacAlgorithm.HS256).build();
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    public TokenEmitido emitir(Usuario usuario, boolean conSegundoFactor) {
        Instant ahora = reloj.instant();
        Instant expira = ahora.plus(propiedades.seguridad().horasToken(), ChronoUnit.HOURS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(usuario.getId().toString())
                .issuedAt(ahora)
                .expiresAt(expira)
                .claim(SeguridadConfig.CLAIM_ROL, usuario.getRol().name())
                .claim("nombre", usuario.getNombre())
                .claim(CLAIM_FACTORES, conSegundoFactor ? List.of(FACTOR_CONTRASENA, FACTOR_OTP)
                        : List.of(FACTOR_CONTRASENA))
                .build();
        return new TokenEmitido(firmar(claims), expira);
    }

    public TokenEmitido emitirTemporal(Usuario usuario) {
        Instant ahora = reloj.instant();
        Instant expira = ahora.plus(MINUTOS_TOKEN_TEMPORAL, ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(usuario.getId().toString())
                .issuedAt(ahora)
                .expiresAt(expira)
                .claim(SeguridadConfig.CLAIM_ETAPA, SeguridadConfig.ETAPA_SEGUNDO_FACTOR)
                .build();
        return new TokenEmitido(firmar(claims), expira);
    }

    /** Devuelve el id del usuario si el token temporal es auténtico, vigente y de la etapa correcta. */
    public Long validarTemporal(String token) {
        try {
            Jwt jwt = decodificadorTemporal.decode(token);
            if (!SeguridadConfig.ETAPA_SEGUNDO_FACTOR.equals(jwt.getClaimAsString(SeguridadConfig.CLAIM_ETAPA))) {
                throw new NoAutenticadoException("El token no corresponde a la verificación en dos pasos.");
            }
            return Long.valueOf(jwt.getSubject());
        } catch (JwtException e) {
            throw new NoAutenticadoException("La verificación expiró. Vuelve a iniciar sesión.");
        }
    }

    public static boolean usoSegundoFactor(Jwt jwt) {
        List<String> factores = jwt.getClaimAsStringList(CLAIM_FACTORES);
        return factores != null && factores.contains(FACTOR_OTP);
    }

    private String firmar(JwtClaimsSet claims) {
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    public record TokenEmitido(String token, Instant expiraEn) {
    }
}
