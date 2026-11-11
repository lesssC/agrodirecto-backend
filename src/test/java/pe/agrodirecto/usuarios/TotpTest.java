package pe.agrodirecto.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.agrodirecto.usuarios.seguridad.Base32;
import pe.agrodirecto.usuarios.seguridad.Totp;

/** Prueba unitaria contra los vectores oficiales del RFC 6238 (apéndice B, HMAC-SHA1). */
class TotpTest {

    private static final byte[] SECRETO_RFC = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

    @ParameterizedTest
    @CsvSource({
        "59, 94287082",
        "1111111109, 07081804",
        "1111111111, 14050471",
        "1234567890, 89005924",
        "2000000000, 69279037",
        "20000000000, 65353130"
    })
    void cumpleLosVectoresDelRfc6238(long segundos, String esperado) {
        assertThat(Totp.codigo(SECRETO_RFC, Totp.paso(Instant.ofEpochSecond(segundos)), 8)).isEqualTo(esperado);
    }

    @Test
    void base32IdaYVuelta() {
        assertThat(Base32.codificar(SECRETO_RFC)).isEqualTo("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");
        assertThat(Base32.decodificar("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")).isEqualTo(SECRETO_RFC);
    }

    @Test
    void aceptaElIntervaloVecinoYRechazaLosDemas() {
        String secreto = Totp.nuevoSecreto();
        Instant ahora = Instant.parse("2026-11-10T15:00:10Z");
        String codigo = Totp.codigoActual(secreto, ahora);

        assertThat(Totp.verificar(secreto, codigo, ahora)).hasValue(Totp.paso(ahora));
        assertThat(Totp.verificar(secreto, codigo, ahora.plusSeconds(30))).isPresent();
        assertThat(Totp.verificar(secreto, codigo, ahora.plusSeconds(90))).isEmpty();
        assertThat(Totp.verificar(secreto, "12345", ahora)).isEmpty();
        assertThat(Totp.verificar(secreto, "abcdef", ahora)).isEmpty();
    }

    @Test
    void generaLaUriParaElCodigoQr() {
        assertThat(Totp.uri("AgroDirecto", "admin@agrodirecto.pe", "ABC234"))
                .isEqualTo("otpauth://totp/AgroDirecto%3Aadmin%40agrodirecto.pe?secret=ABC234&issuer=AgroDirecto"
                        + "&algorithm=SHA1&digits=6&period=30");
    }
}
