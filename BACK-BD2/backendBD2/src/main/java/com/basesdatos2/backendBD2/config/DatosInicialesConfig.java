package com.basesdatos2.backendBD2.config;

import com.basesdatos2.backendBD2.modelo.Usuario;
import com.basesdatos2.backendBD2.modelo.emun.ModuloLog;
import com.basesdatos2.backendBD2.modelo.emun.Rol;
import com.basesdatos2.backendBD2.modelo.emun.TipoDocumento;
import com.basesdatos2.backendBD2.repositorio.LogBucketRepository;
import com.basesdatos2.backendBD2.repositorio.LogRepository;
import com.basesdatos2.backendBD2.repositorio.UsuarioRepository;
import com.basesdatos2.backendBD2.servicio.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

// Se ejecuta una vez al arrancar el back (en segundo plano para no bloquear el arranque):
//  1) crea los indices de la coleccion de logs en MongoDB;
//  2) asegura que USUARIO.Telefono sea BIGINT (para celulares de 10 digitos);
//  3) crea el usuario ADMIN (el unico que ve los logs) si todavia no existe ninguno;
//  4) si la coleccion de logs en MongoDB esta vacia, registra los eventos iniciales de auditoria.
@Component
public class DatosInicialesConfig implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DatosInicialesConfig.class);

    private final LogRepository logRepository;
    private final LogBucketRepository logBucketRepository;
    private final LogService logService;
    private final UsuarioRepository usuarioRepository;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Integer documentoAdmin;
    private final String correoAdmin;
    private final String contrasenaAdmin;

    public DatosInicialesConfig(LogRepository logRepository,
                                LogBucketRepository logBucketRepository,
                                LogService logService,
                                UsuarioRepository usuarioRepository,
                                JdbcTemplate jdbcTemplate,
                                BCryptPasswordEncoder passwordEncoder,
                                @Value("${app.admin.documento}") Integer documentoAdmin,
                                @Value("${app.admin.correo}") String correoAdmin,
                                @Value("${app.admin.contrasena}") String contrasenaAdmin) {
        this.logRepository = logRepository;
        this.logBucketRepository = logBucketRepository;
        this.logService = logService;
        this.usuarioRepository = usuarioRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.documentoAdmin = documentoAdmin;
        this.correoAdmin = correoAdmin;
        this.contrasenaAdmin = contrasenaAdmin;
    }

    @Async
    @Override
    public void run(ApplicationArguments args) {
        try {
            logRepository.crearIndices();
        } catch (RuntimeException e) {
            log.warn("No se pudieron crear los indices de logs en MongoDB: {}", e.getMessage());
        }

        try {
            usuarioRepository.asegurarEsquemaTelefonoBigInt();
        } catch (RuntimeException e) {
            log.warn("No se pudo verificar esquema BIGINT para Telefono: {}", e.getMessage());
        }

        try {
            if (!usuarioRepository.existeConRol(Rol.ADMIN)) {
                usuarioRepository.registrar(adminInicial());
                log.info("Usuario ADMIN creado con hash BCrypt: {}", correoAdmin);
            }
            migrarContrasenasEnTextoPlano();
        } catch (RuntimeException e) {
            log.warn("No se pudo verificar/crear el usuario ADMIN: {}", e.getMessage());
        }

        try {
            inicializarLogsBaseSiVacio();
        } catch (RuntimeException e) {
            log.warn("No se pudieron inicializar los logs base en MongoDB: {}", e.getMessage());
        }
    }

    private void inicializarLogsBaseSiVacio() {
        if (logBucketRepository.count() > 0) {
            return;
        }
        for (Usuario u : usuarioRepository.listarTodos()) {
            Map<String, Object> detalle = new LinkedHashMap<>();
            detalle.put("tipoDocumento", u.getTipoDocumento());
            detalle.put("numeroDocumento", u.getNumeroDocumento());
            detalle.put("nombreCompleto", u.nombreCompleto());
            detalle.put("correo", u.getCorreo());
            detalle.put("telefono", u.getTelefono());
            detalle.put("rol", u.getRol());
            detalle.put("fechaRegistro", u.getFechaRegistro());

            logService.registrar(
                    ModuloLog.USUARIO,
                    "REGISTRADO",
                    u.getNumeroDocumento(),
                    "Usuario #" + u.getNumeroDocumento(),
                    "Usuario activo en plataforma: " + u.nombreCompleto() + " (" + u.getCorreo() + ") con rol " + u.getRol() + ".",
                    detalle
            );
        }

        jdbcTemplate.query(
                "SELECT TRIM(Placa) AS Placa, TRIM(Marca) AS Marca, TRIM(modelo) AS Modelo, Año, Precio_alquiler, TRIM(Estado_motocicleta) AS Estado, Numero_documento FROM MOTOCICLETA",
                rs -> {
                    String placa = rs.getString("Placa");
                    String marca = rs.getString("Marca");
                    String modelo = rs.getString("Modelo");
                    int precio = rs.getInt("Precio_alquiler");
                    String estado = rs.getString("Estado");
                    int doc = rs.getInt("Numero_documento");
                    Map<String, Object> det = new LinkedHashMap<>();
                    det.put("placa", placa);
                    det.put("marca", marca);
                    det.put("modelo", modelo);
                    det.put("anio", rs.getInt("Año"));
                    det.put("precioDia", precio);
                    det.put("estado", estado);
                    logService.registrar(ModuloLog.MOTO, "PUBLICADA", doc, placa,
                            "Publicó la moto " + marca + " " + modelo + " (" + placa + ") a " + LogService.pesos(precio) + "/día.",
                            det);
                }
        );

        jdbcTemplate.query(
                "SELECT Id_solicitud, TRIM(Placa) AS Placa, TRIM(Estado_solicitud) AS Estado, Fecha_solicitud, Numero_documento FROM SOLICITUD",
                rs -> {
                    int idSol = rs.getInt("Id_solicitud");
                    String placa = rs.getString("Placa");
                    String estado = rs.getString("Estado");
                    int doc = rs.getInt("Numero_documento");
                    Map<String, Object> det = new LinkedHashMap<>();
                    det.put("idSolicitud", idSol);
                    det.put("placa", placa);
                    det.put("estado", estado);
                    det.put("fechaSolicitud", rs.getDate("Fecha_solicitud").toLocalDate());
                    logService.registrar(ModuloLog.SOLICITUD, estado, doc, "Solicitud #" + idSol,
                            "Solicitud #" + idSol + " para la moto " + placa + " (" + estado + ").",
                            det);
                }
        );

        jdbcTemplate.query(
                "SELECT id_contrato, TRIM(Placa) AS Placa, TRIM(Estado_contrato) AS Estado, Fecha_inicio, Fecha_fin, Deposito, Numero_documento1 FROM CONTRATO",
                rs -> {
                    int idCon = rs.getInt("id_contrato");
                    String placa = rs.getString("Placa");
                    String estado = rs.getString("Estado");
                    int doc = rs.getInt("Numero_documento1");
                    Map<String, Object> det = new LinkedHashMap<>();
                    det.put("idContrato", idCon);
                    det.put("placa", placa);
                    det.put("estado", estado);
                    det.put("fechaInicio", rs.getDate("Fecha_inicio").toLocalDate());
                    det.put("fechaFin", rs.getDate("Fecha_fin").toLocalDate());
                    det.put("deposito", rs.getInt("Deposito"));
                    logService.registrar(ModuloLog.CONTRATO, estado, doc, "Contrato #" + idCon,
                            "Contrato #" + idCon + " de la moto " + placa + " (" + estado + ").",
                            det);
                }
        );

        jdbcTemplate.query(
                "SELECT Id_pago, id_contrato, Monto, TRIM(Estado) AS Estado, TRIM(Metodo_pago) AS Metodo, Numero_documento FROM PAGO",
                rs -> {
                    int idPago = rs.getInt("Id_pago");
                    int idCon = rs.getInt("id_contrato");
                    int monto = rs.getInt("Monto");
                    String estado = rs.getString("Estado");
                    String metodo = rs.getString("Metodo");
                    int doc = rs.getInt("Numero_documento");
                    Map<String, Object> det = new LinkedHashMap<>();
                    det.put("idPago", idPago);
                    det.put("idContrato", idCon);
                    det.put("monto", monto);
                    det.put("estado", estado);
                    det.put("metodoPago", metodo);
                    logService.registrar(ModuloLog.PAGO, estado, doc, "Pago #" + idPago,
                            "Pago #" + idPago + " del contrato #" + idCon + " por " + LogService.pesos(monto) + " (" + estado + ").",
                            det);
                }
        );

        log.info("Coleccion logs_actividad inicializada en MongoDB con todos los eventos del sistema.");
    }

    private void migrarContrasenasEnTextoPlano() {
        for (Usuario u : usuarioRepository.listarConContrasenaSinHashear()) {
            if (u.getContrasena() != null && !u.getContrasena().isBlank()) {
                usuarioRepository.actualizarContrasena(
                        u.getNumeroDocumento(),
                        passwordEncoder.encode(u.getContrasena()));
                log.info("Contraseña en texto plano migrada a BCrypt para el usuario {}", u.getCorreo());
            }
        }
    }

    private Usuario adminInicial() {
        Usuario admin = Usuario.segunRol(Rol.ADMIN);
        admin.setTipoDocumento(TipoDocumento.CC);
        admin.setNumeroDocumento(documentoAdmin);
        admin.setPrimerNombre("Administrador");
        admin.setPrimerApellido("MotoGo");
        admin.setTelefono(1L);
        admin.setCorreo(correoAdmin);
        admin.setContrasena(passwordEncoder.encode(contrasenaAdmin));
        return admin;
    }
}
