package pe.agrodirecto.productores.dominio;

import java.util.regex.Pattern;

/** Medios por los que el agricultor recibe su dinero (RN-C2): los que ya usa en el campo. */
public enum MetodoCobro {
    YAPE("9\\d{8}", "un celular de 9 dígitos"),
    PLIN("9\\d{8}", "un celular de 9 dígitos"),
    /** Cuenta DNI del Banco de la Nación: el número de cuenta es el propio DNI. */
    CUENTA_DNI("\\d{8}", "el DNI de 8 dígitos"),
    /** Código de cuenta interbancario. */
    CUENTA_BANCARIA("\\d{20}", "el CCI de 20 dígitos");

    private final Pattern formato;
    private final String descripcion;

    MetodoCobro(String formato, String descripcion) {
        this.formato = Pattern.compile(formato);
        this.descripcion = descripcion;
    }

    public boolean numeroValido(String numero) {
        return numero != null && formato.matcher(numero).matches();
    }

    public String descripcionFormato() {
        return descripcion;
    }
}
