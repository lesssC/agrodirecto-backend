package pe.agrodirecto.compartido;

import java.util.List;
import java.util.Optional;

/** Las 25 regiones del Perú (24 departamentos y la Provincia Constitucional del Callao). */
public final class Regiones {

    public static final List<String> TODAS = List.of(
            "Amazonas", "Áncash", "Apurímac", "Arequipa", "Ayacucho", "Cajamarca", "Callao", "Cusco",
            "Huancavelica", "Huánuco", "Ica", "Junín", "La Libertad", "Lambayeque", "Lima", "Loreto",
            "Madre de Dios", "Moquegua", "Pasco", "Piura", "Puno", "San Martín", "Tacna", "Tumbes", "Ucayali");

    private Regiones() {
    }

    /** Devuelve el nombre oficial aunque se escriba sin tildes o con otras mayúsculas ("junin" → "Junín"). */
    public static Optional<String> normalizar(String region) {
        if (region == null) {
            return Optional.empty();
        }
        String clave = Texto.normalizar(region);
        return TODAS.stream().filter(r -> Texto.normalizar(r).equals(clave)).findFirst();
    }
}
