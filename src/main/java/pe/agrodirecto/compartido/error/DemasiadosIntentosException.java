package pe.agrodirecto.compartido.error;

/** Cuenta bloqueada temporalmente por intentos fallidos de inicio de sesión. */
public class DemasiadosIntentosException extends RuntimeException {

    public DemasiadosIntentosException(String mensaje) {
        super(mensaje);
    }
}
