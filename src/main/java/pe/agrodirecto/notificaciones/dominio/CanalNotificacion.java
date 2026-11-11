package pe.agrodirecto.notificaciones.dominio;

public enum CanalNotificacion {
    /** Mensaje de texto al celular del agricultor; funciona en cualquier teléfono, incluso sin internet. */
    SMS,
    /** Enlace wa.me listo para enviar: el agente lo abre y lo manda gratis desde su WhatsApp. */
    WHATSAPP,
    /** Aviso en la campana de la web para usuarios con cuenta. */
    WEB
}
