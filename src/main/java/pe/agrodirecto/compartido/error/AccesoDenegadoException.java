package pe.agrodirecto.compartido.error;

/** El usuario está autenticado pero su rol o relación con el recurso no le permite la operación (RN-07). */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
