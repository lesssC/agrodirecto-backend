package pe.agrodirecto.administracion.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.agrodirecto.administracion.dominio.AccionAdministrativa;
import pe.agrodirecto.administracion.dominio.TipoAccion;
import pe.agrodirecto.administracion.repositorio.AccionAdministrativaRepository;
import pe.agrodirecto.administracion.web.AccionResponse;
import pe.agrodirecto.catalogo.servicio.PrecioReferenciaService;
import pe.agrodirecto.catalogo.web.PrecioReferenciaRequest;
import pe.agrodirecto.catalogo.web.PrecioReferenciaResponse;
import pe.agrodirecto.compartido.error.NoEncontradoException;
import pe.agrodirecto.compartido.error.ReglaNegocioException;
import pe.agrodirecto.productores.dominio.Productor;
import pe.agrodirecto.productores.servicio.ProductorService;
import pe.agrodirecto.productores.web.ProductorGestionResponse;
import pe.agrodirecto.usuarios.dominio.EstadoCuenta;
import pe.agrodirecto.usuarios.dominio.Rol;
import pe.agrodirecto.usuarios.dominio.Usuario;
import pe.agrodirecto.usuarios.repositorio.UsuarioRepository;
import pe.agrodirecto.usuarios.servicio.UsuarioActualService;
import pe.agrodirecto.usuarios.web.UsuarioResponse;

/**
 * Control de la plataforma (RN-17, RN-23). Cada acción exige un motivo y queda en la auditoría.
 */
@Service
@RequiredArgsConstructor
public class AdministracionService {

    private static final String USUARIO = "USUARIO";
    private static final String PRODUCTOR = "PRODUCTOR";
    private static final String PRECIO = "PRECIO_REFERENCIA";

    private final UsuarioRepository usuarios;
    private final ProductorService productorService;
    private final PrecioReferenciaService precioReferenciaService;
    private final AccionAdministrativaRepository auditoria;
    private final UsuarioActualService usuarioActual;
    private final Clock reloj;

    // ---------- Usuarios ----------

    @Transactional(readOnly = true)
    public List<UsuarioResponse> usuarios(Jwt jwt, Rol rol, Boolean verificado, EstadoCuenta estado) {
        administrador(jwt);
        return usuarios.findAll().stream()
                .filter(u -> rol == null || u.getRol() == rol)
                .filter(u -> verificado == null || u.isVerificado() == verificado)
                .filter(u -> estado == null || u.getEstado() == estado)
                .sorted((a, b) -> b.getCreadoEn().compareTo(a.getCreadoEn()))
                .map(UsuarioResponse::de).toList();
    }

    /**
     * RN-17: la identidad de agentes y compradores se valida fuera de la plataforma (DNI, RUC en SUNAT).
     * Un agente verificado puede registrar agricultores; un comprador verificado ya no tiene los topes de cuenta nueva.
     */
    @Transactional
    public UsuarioResponse verificar(Jwt jwt, Long id, String motivo) {
        Usuario admin = administrador(jwt);
        Usuario usuario = buscarUsuario(id);
        if (usuario.getRol() != Rol.AGENTE && usuario.getRol() != Rol.COMPRADOR) {
            throw new ReglaNegocioException("RN-17", "Se verifican cuentas de agentes y compradores; los agricultores "
                    + "se verifican en persona desde su perfil.");
        }
        usuario.setVerificado(true);
        registrar(admin, TipoAccion.VERIFICAR_USUARIO, USUARIO, id, motivo);
        return UsuarioResponse.de(usuario);
    }

    /** RN-23: la suspensión corta el acceso de inmediato, incluso con un token vigente. */
    @Transactional
    public UsuarioResponse suspender(Jwt jwt, Long id, String motivo) {
        Usuario admin = administrador(jwt);
        Usuario usuario = buscarUsuario(id);
        if (usuario.getRol() == Rol.ADMINISTRADOR) {
            throw new ReglaNegocioException("RN-07", "Una cuenta de administración no se suspende desde el panel.");
        }
        usuario.setEstado(EstadoCuenta.SUSPENDIDO);
        registrar(admin, TipoAccion.SUSPENDER_USUARIO, USUARIO, id, motivo);
        return UsuarioResponse.de(usuario);
    }

    @Transactional
    public UsuarioResponse reactivar(Jwt jwt, Long id, String motivo) {
        Usuario admin = administrador(jwt);
        Usuario usuario = buscarUsuario(id);
        usuario.setEstado(EstadoCuenta.ACTIVO);
        registrar(admin, TipoAccion.REACTIVAR_USUARIO, USUARIO, id, motivo);
        return UsuarioResponse.de(usuario);
    }

    // ---------- Productores ----------

    /** Un productor suspendido desaparece del catálogo y del directorio, pero conserva su historial. */
    @Transactional
    public ProductorGestionResponse suspenderProductor(Jwt jwt, Long id, String motivo) {
        Usuario admin = administrador(jwt);
        Productor productor = productorService.buscar(id);
        productor.setEstado(EstadoCuenta.SUSPENDIDO);
        registrar(admin, TipoAccion.SUSPENDER_PRODUCTOR, PRODUCTOR, id, motivo);
        return ProductorGestionResponse.de(productor);
    }

    @Transactional
    public ProductorGestionResponse reactivarProductor(Jwt jwt, Long id, String motivo) {
        Usuario admin = administrador(jwt);
        Productor productor = productorService.buscar(id);
        productor.setEstado(EstadoCuenta.ACTIVO);
        registrar(admin, TipoAccion.REACTIVAR_PRODUCTOR, PRODUCTOR, id, motivo);
        return ProductorGestionResponse.de(productor);
    }

    /**
     * Protección al agricultor: si su agente es sancionado o deja de colaborar, el agricultor no pierde su
     * vitrina ni sus ventas; se le asigna otro agente verificado.
     */
    @Transactional
    public ProductorGestionResponse reasignarAgente(Jwt jwt, Long productorId, Long agenteId, String motivo) {
        Usuario admin = administrador(jwt);
        Productor productor = productorService.buscar(productorId);
        Usuario agente = buscarUsuario(agenteId);
        if (agente.getRol() != Rol.AGENTE || !agente.isVerificado() || !agente.estaActivo()) {
            throw new ReglaNegocioException("RN-B1", "El nuevo responsable debe ser un agente verificado y activo.");
        }
        String anterior = productor.getAgente() == null ? "sin agente" : productor.getAgente().getNombre();
        productor.setAgente(agente);
        registrar(admin, TipoAccion.REASIGNAR_AGENTE, PRODUCTOR, productorId,
                anterior + " → " + agente.getNombre() + ". " + motivo);
        return ProductorGestionResponse.de(productor);
    }

    // ---------- Precios de referencia ----------

    @Transactional
    public PrecioReferenciaResponse crearPrecio(Jwt jwt, PrecioReferenciaRequest solicitud) {
        Usuario admin = administrador(jwt);
        PrecioReferenciaResponse precio = precioReferenciaService.crear(solicitud);
        registrar(admin, TipoAccion.CREAR_PRECIO_REFERENCIA, PRECIO, precio.id(), resumenPrecio(solicitud));
        return precio;
    }

    @Transactional
    public PrecioReferenciaResponse actualizarPrecio(Jwt jwt, Long id, PrecioReferenciaRequest solicitud) {
        Usuario admin = administrador(jwt);
        PrecioReferenciaResponse precio = precioReferenciaService.actualizar(id, solicitud);
        registrar(admin, TipoAccion.ACTUALIZAR_PRECIO_REFERENCIA, PRECIO, id, resumenPrecio(solicitud));
        return precio;
    }

    // ---------- Auditoría ----------

    @Transactional(readOnly = true)
    public List<AccionResponse> auditoria(Jwt jwt, String objetivo, Long objetivoId) {
        administrador(jwt);
        List<AccionAdministrativa> acciones = objetivo != null && objetivoId != null
                ? auditoria.findByObjetivoAndObjetivoIdOrderByCreadoEnDescIdDesc(objetivo, objetivoId)
                : auditoria.findTop100ByOrderByCreadoEnDescIdDesc();
        return acciones.stream().map(AccionResponse::de).toList();
    }

    /** Además del filtro por ruta, confirma que la cuenta sigue activa (un token puede seguir vigente). */
    public Usuario exigirAdministrador(Jwt jwt) {
        return usuarioActual.exigirRol(jwt, Rol.ADMINISTRADOR);
    }

    // ---------- Internos ----------

    private Usuario administrador(Jwt jwt) {
        return exigirAdministrador(jwt);
    }

    private Usuario buscarUsuario(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new NoEncontradoException("Usuario", id));
    }

    private void registrar(Usuario admin, TipoAccion tipo, String objetivo, Long objetivoId, String detalle) {
        AccionAdministrativa accion = new AccionAdministrativa();
        accion.setAdministrador(admin);
        accion.setTipo(tipo);
        accion.setObjetivo(objetivo);
        accion.setObjetivoId(objetivoId);
        accion.setDetalle(detalle == null || detalle.length() <= 500 ? detalle : detalle.substring(0, 500));
        accion.setCreadoEn(LocalDateTime.now(reloj));
        auditoria.save(accion);
    }

    private static String resumenPrecio(PrecioReferenciaRequest s) {
        return s.clave() + ": mayorista S/ " + s.precioMayoristaKg() + "/kg, minorista S/ " + s.precioMinoristaKg()
                + "/kg (" + s.fuente() + ", " + s.fecha() + ")";
    }
}
