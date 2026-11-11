package pe.agrodirecto.usuarios.seguridad;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import pe.agrodirecto.config.AgroDirectoProperties;

/**
 * Cifra los secretos TOTP antes de guardarlos (AES-256-GCM). Si se filtrara la base de datos, los secretos
 * no servirían sin la clave, que vive fuera de ella (variable de entorno {@code TOTP_CLAVE}).
 */
@Component
public class CifradoSecretos {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final SecretKey clave;

    public CifradoSecretos(AgroDirectoProperties propiedades) {
        this.clave = derivar(propiedades.seguridad().claveCifrado());
    }

    public String cifrar(String texto) {
        try {
            byte[] iv = new byte[IV_BYTES];
            ALEATORIO.nextBytes(iv);
            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cifrado = cifrador.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + cifrado.length)
                    .put(iv).put(cifrado).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el secreto", e);
        }
    }

    public String descifrar(String guardado) {
        try {
            byte[] datos = Base64.getDecoder().decode(guardado);
            Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, datos, 0, IV_BYTES));
            return new String(cifrador.doFinal(datos, IV_BYTES, datos.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo descifrar el secreto (¿cambió TOTP_CLAVE?)", e);
        }
    }

    private static SecretKey derivar(String frase) {
        if (frase == null || frase.length() < 32) {
            throw new IllegalStateException("agrodirecto.seguridad.clave-cifrado debe tener al menos 32 caracteres.");
        }
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(frase.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(bytes, "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
