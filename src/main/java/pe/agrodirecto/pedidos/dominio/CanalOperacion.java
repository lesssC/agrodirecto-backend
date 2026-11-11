package pe.agrodirecto.pedidos.dominio;

/** Por dónde se realizó un cambio de estado: queda registrado en el historial. */
public enum CanalOperacion {
    WEB,
    /** Respuesta del agricultor desde un celular básico (RN-B2). */
    SMS,
    /** Acción automática de la plataforma. */
    SISTEMA
}
