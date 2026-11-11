package pe.agrodirecto.catalogo.dominio;

public enum EstadoProducto {
    /** Visible en el catálogo y disponible para pedidos. */
    ACTIVO,
    /** Guardado sin publicar; solo lo ve quien lo gestiona. */
    BORRADOR,
    /** Retirado por el productor; se conserva por los pedidos históricos. */
    RETIRADO
}
