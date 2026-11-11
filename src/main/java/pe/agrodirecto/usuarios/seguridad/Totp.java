package pe.agrodirecto.usuarios.seguridad;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.OptionalLong;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Contraseña de un solo uso basada en el tiempo (TOTP, RFC 6238), compatible con Google Authenticator,
 * Microsoft Authenticator o Authy. Cada 30 segundos se genera un código de 6 dígitos a partir de un secreto
 * compartido y la hora actual; no necesita internet ni SMS, por eso no tiene costo.
 */
public final class Totp {

    public static final int DIGITOS = 6;
    public static final int PERIODO_SEGUNDOS = 30;
    /** Se acepta el código del intervalo anterior y del siguiente para tolerar relojes desfasados. */
    private static final int VENTANA = 1;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Totp() {
    }

    /** 160 bits aleatorios, el tamaño recomendado para HMAC-SHA1. */
    public static String nuevoSecreto() {
        byte[] secreto = new byte[20];
        ALEATORIO.nextBytes(secreto);
        return Base32.codificar(secreto);
    }

    public static long paso(Instant instante) {
        return instante.getEpochSecond() / PERIODO_SEGUNDOS;
    }

    /** Código para un intervalo concreto (RFC 4226, truncamiento dinámico). */
    public static String codigo(byte[] secreto, long paso, int digitos) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA1");
            hmac.init(new SecretKeySpec(secreto, "HmacSHA1"));
            byte[] hash = hmac.doFinal(ByteBuffer.allocate(8).putLong(paso).array());
            int desplazamiento = hash[hash.length - 1] & 0x0F;
            int binario = ((hash[desplazamiento] & 0x7F) << 24) | ((hash[desplazamiento + 1] & 0xFF) << 16)
                    | ((hash[desplazamiento + 2] & 0xFF) << 8) | (hash[desplazamiento + 3] & 0xFF);
            int modulo = (int) Math.pow(10, digitos);
            return String.format("%0" + digitos + "d", binario % modulo);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA1 no disponible", e);
        }
    }

    public static String codigoActual(String secretoBase32, Instant instante) {
        return codigo(Base32.decodificar(secretoBase32), paso(instante), DIGITOS);
    }

    /**
     * Devuelve el intervalo al que corresponde el código, o vacío si no es válido.
     * Con ese intervalo se impide reutilizar un código ya usado (protección contra repetición).
     */
    public static OptionalLong verificar(String secretoBase32, String codigo, Instant instante) {
        if (codigo == null || !codigo.matches("\\d{" + DIGITOS + "}")) {
            return OptionalLong.empty();
        }
        byte[] secreto = Base32.decodificar(secretoBase32);
        long actual = paso(instante);
        for (long p = actual - VENTANA; p <= actual + VENTANA; p++) {
            // Comparación en tiempo constante para no revelar dígitos correctos por el tiempo de respuesta.
            if (MessageDigest.isEqual(codigo(secreto, p, DIGITOS).getBytes(), codigo.getBytes())) {
                return OptionalLong.of(p);
            }
        }
        return OptionalLong.empty();
    }

    /** URI que el frontend convierte en código QR para escanearlo con la app. */
    public static String uri(String emisor, String cuenta, String secretoBase32) {
        String etiqueta = java.net.URLEncoder.encode(emisor + ":" + cuenta, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "otpauth://totp/" + etiqueta + "?secret=" + secretoBase32 + "&issuer=" + emisor
                + "&algorithm=SHA1&digits=" + DIGITOS + "&period=" + PERIODO_SEGUNDOS;
    }
}
