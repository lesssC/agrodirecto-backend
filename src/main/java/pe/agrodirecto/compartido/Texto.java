package pe.agrodirecto.compartido;

import java.text.Normalizer;
import java.util.Locale;

public final class Texto {

    private Texto() {
    }

    /** Minúsculas y sin tildes: "Café Orgánico" → "cafe organico". Para búsquedas que ignoran acentos. */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return sinTildes(texto.trim()).toLowerCase(Locale.ROOT);
    }

    /**
     * Quita tildes conservando mayúsculas: "Recibirás" → "Recibiras". Los SMS en alfabeto GSM básico
     * admiten más caracteres por mensaje y se leen bien en cualquier celular.
     */
    public static String sinTildes(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
