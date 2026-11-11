package pe.agrodirecto.usuarios.dominio;

public enum Rol {
    COMPRADOR,
    PRODUCTOR,
    /** Promotor de campo, familiar o asociación que opera la plataforma en nombre de agricultores sin computadora (RN-B1). */
    AGENTE,
    ADMINISTRADOR;

    /** Roles que cualquier persona puede elegir al registrarse; el administrador no se autoregistra (RN-07). */
    public boolean permiteAutoregistro() {
        return this != ADMINISTRADOR;
    }
}
