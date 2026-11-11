package pe.agrodirecto.usuarios.dominio;

import java.time.Duration;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(length = 20)
    private String telefono;

    @Column(length = 8)
    private String dni;

    @Column(length = 11)
    private String ruc;

    @Column(name = "razon_social", length = 160)
    private String razonSocial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    /** Identidad validada (DNI o RUC) por la administración (RN-17). */
    @Column(nullable = false)
    private boolean verificado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCuenta estado = EstadoCuenta.ACTIVO;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    /** Secreto TOTP cifrado. Puede existir sin estar activo mientras el usuario termina de configurarlo. */
    @Column(name = "totp_secreto", length = 255)
    private String totpSecreto;

    @Column(name = "totp_activo", nullable = false)
    private boolean totpActivo;

    /** Último intervalo de 30 s aceptado: un código ya usado no vale dos veces. */
    @Column(name = "totp_ultimo_paso")
    private Long totpUltimoPaso;

    public boolean estaActivo() {
        return estado == EstadoCuenta.ACTIVO;
    }

    public boolean estaBloqueado(LocalDateTime ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    /** Suma un intento fallido; al alcanzar el máximo, bloquea la cuenta temporalmente. */
    public void registrarIntentoFallido(LocalDateTime ahora, int maximoIntentos, Duration bloqueo) {
        intentosFallidos++;
        if (intentosFallidos >= maximoIntentos) {
            bloqueadoHasta = ahora.plus(bloqueo);
            intentosFallidos = 0;
        }
    }

    public void registrarAccesoExitoso(LocalDateTime ahora) {
        intentosFallidos = 0;
        bloqueadoHasta = null;
        ultimoAcceso = ahora;
    }
}
