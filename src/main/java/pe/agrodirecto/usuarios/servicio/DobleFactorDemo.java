package pe.agrodirecto.usuarios.servicio;

import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.agrodirecto.config.AgroDirectoProperties;
import pe.agrodirecto.usuarios.repositorio.UsuarioRepository;
import pe.agrodirecto.usuarios.seguridad.CifradoSecretos;

/**
 * Solo para la demostración: activa el segundo factor de las cuentas demo con secretos conocidos, para poder
 * agregarlos a una app de autenticación durante la presentación. Se desactiva con DOBLE_FACTOR_DEMO=false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DobleFactorDemo implements ApplicationRunner {

    /** Secretos públicos de las cuentas demo (Base32). Nunca usar en producción. */
    public static final Map<String, String> SECRETOS = Map.of(
            "admin@agrodirecto.pe", "AGRODIRECTOADMINDEMO234567ABCDEF",
            "promotora@agrodirecto.pe", "AGRODIRECTOAGENTEDEMO234567ABCDE");

    private final UsuarioRepository usuarios;
    private final CifradoSecretos cifrado;
    private final AgroDirectoProperties propiedades;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!propiedades.seguridad().dobleFactorDemo()) {
            return;
        }
        SECRETOS.forEach((email, secreto) -> usuarios.findByEmailIgnoreCase(email)
                .filter(u -> !u.isTotpActivo())
                .ifPresent(u -> {
                    u.setTotpSecreto(cifrado.cifrar(secreto));
                    u.setTotpActivo(true);
                    log.warn("Segundo factor de DEMOSTRACIÓN activado para {}. Desactívalo en producción "
                            + "(DOBLE_FACTOR_DEMO=false).", email);
                }));
    }
}
