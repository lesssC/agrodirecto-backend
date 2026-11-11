package pe.agrodirecto.notificaciones.servicio;

/**
 * Puerto de salida para enviar SMS. Hoy lo implementa {@link EnvioSmsSimulado}; para producción basta
 * con otra implementación que llame a un proveedor (por ejemplo, un operador local o Twilio) sin tocar el resto.
 */
public interface EnvioSms {

    void enviar(String celular, String texto);
}
