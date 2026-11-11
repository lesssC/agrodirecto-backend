package pe.agrodirecto.usuarios.web;

import java.time.Instant;

/**
 * Respuesta del login.
 * <ul>
 * <li>Sin segundo factor: {@code token} listo para usar como {@code Authorization: Bearer <token>}.</li>
 * <li>Con segundo factor: {@code requiereSegundoFactor = true}, {@code token = null} y un {@code tokenTemporal}
 * que se envía junto al código de 6 dígitos a {@code /api/auth/2fa/verificar}.</li>
 * </ul>
 */
public record SesionResponse(String token, String tipo, Instant expiraEn, UsuarioResponse usuario,
        boolean requiereSegundoFactor, String tokenTemporal) {

    public static SesionResponse completa(String token, Instant expiraEn, UsuarioResponse usuario) {
        return new SesionResponse(token, "Bearer", expiraEn, usuario, false, null);
    }

    public static SesionResponse pendienteDeCodigo(String tokenTemporal, Instant expiraEn) {
        return new SesionResponse(null, null, expiraEn, null, true, tokenTemporal);
    }
}
