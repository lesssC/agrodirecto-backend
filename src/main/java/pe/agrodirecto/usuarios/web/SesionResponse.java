package pe.agrodirecto.usuarios.web;

import java.time.Instant;

/** Respuesta del login: el frontend guarda {@code token} y lo envía como {@code Authorization: Bearer <token>}. */
public record SesionResponse(String token, String tipo, Instant expiraEn, UsuarioResponse usuario) {

    public SesionResponse(String token, Instant expiraEn, UsuarioResponse usuario) {
        this(token, "Bearer", expiraEn, usuario);
    }
}
