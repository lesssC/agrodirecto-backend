package pe.agrodirecto.confianza.dominio;

public enum MotivoDisputa {
    NO_ENTREGADO(false),
    CALIDAD(true),
    CANTIDAD(true),
    PRODUCTO_DISTINTO(true),
    OTRO(false);

    /** RN-15: los reclamos sobre lo recibido necesitan una foto como evidencia. */
    private final boolean requiereEvidencia;

    MotivoDisputa(boolean requiereEvidencia) {
        this.requiereEvidencia = requiereEvidencia;
    }

    public boolean requiereEvidencia() {
        return requiereEvidencia;
    }
}
