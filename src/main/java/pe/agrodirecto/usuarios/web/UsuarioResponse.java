package pe.agrodirecto.usuarios.web;

import pe.agrodirecto.usuarios.dominio.EstadoCuenta;
import pe.agrodirecto.usuarios.dominio.Rol;
import pe.agrodirecto.usuarios.dominio.Usuario;

public record UsuarioResponse(Long id, String nombre, String email, String telefono, String razonSocial, Rol rol,
        boolean verificado, EstadoCuenta estado, boolean dobleFactorActivo) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getEmail(), u.getTelefono(), u.getRazonSocial(),
                u.getRol(), u.isVerificado(), u.getEstado(), u.isTotpActivo());
    }
}
