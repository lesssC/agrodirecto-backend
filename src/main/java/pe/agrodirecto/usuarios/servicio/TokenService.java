package pe.agrodirecto.usuarios.servicio;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.config.AgroDirectoProperties;
import pe.agrodirecto.config.SeguridadConfig;
import pe.agrodirecto.usuarios.dominio.Usuario;

/** Emite los JWT de acceso. El {@code sub} es el id del usuario y {@code rol} su rol. */
@Service
@RequiredArgsConstructor
public class TokenService {

    public static final String EMISOR = "agrodirecto";

    private final JwtEncoder jwtEncoder;
    private final AgroDirectoProperties propiedades;
    private final Clock reloj;

    public TokenEmitido emitir(Usuario usuario) {
        Instant ahora = reloj.instant();
        Instant expira = ahora.plus(propiedades.seguridad().horasToken(), ChronoUnit.HOURS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(usuario.getId().toString())
                .issuedAt(ahora)
                .expiresAt(expira)
                .claim(SeguridadConfig.CLAIM_ROL, usuario.getRol().name())
                .claim("nombre", usuario.getNombre())
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
        return new TokenEmitido(token, expira);
    }

    public record TokenEmitido(String token, Instant expiraEn) {
    }
}
