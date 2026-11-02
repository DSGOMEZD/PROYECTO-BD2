package com.basesdatos2.backendBD2.servicio.implementacion;

import com.basesdatos2.backendBD2.modelo.LogBucket;
import com.basesdatos2.backendBD2.modelo.LogEvento;
import com.basesdatos2.backendBD2.modelo.Usuario;
import com.basesdatos2.backendBD2.modelo.dto.LogResponseDTO;
import com.basesdatos2.backendBD2.modelo.emun.EstadoUsuario;
import com.basesdatos2.backendBD2.modelo.emun.ModuloLog;
import com.basesdatos2.backendBD2.modelo.emun.Rol;
import com.basesdatos2.backendBD2.modelo.excepcion.NoAutorizadoException;
import com.basesdatos2.backendBD2.modelo.excepcion.ReglaNegocioException;
import com.basesdatos2.backendBD2.repositorio.LogRepository;
import com.basesdatos2.backendBD2.repositorio.UsuarioRepository;
import com.basesdatos2.backendBD2.servicio.LogService;
import jakarta.servlet.http.HttpServletRequest;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LogServiceImpl implements LogService {
    private static final Logger log = LoggerFactory.getLogger(LogServiceImpl.class);
    private static final long MAX_DIAS_CONSULTA = 366;

    private final LogRepository logRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModelMapper mm;

    public LogServiceImpl(LogRepository logRepository,
                          UsuarioRepository usuarioRepository,
                          ModelMapper mm) {
        this.logRepository = logRepository;
        this.usuarioRepository = usuarioRepository;
        this.mm = mm;
    }

    @Override
    public void registrar(ModuloLog modulo,
                          String accion,
                          Integer documentoUsuario,
                          String referencia,
                          String descripcion,
                          Map<String, Object> detalle) {
        try {
            LogEvento evento = new LogEvento();
            evento.setId(new ObjectId().toHexString());
            evento.setFecha(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
            evento.setAccion(accion);
            evento.setDocumentoUsuario(documentoUsuario);
            evento.setReferencia(referencia);
            evento.setDescripcion(descripcion);
            evento.setDetalle(normalizar(detalle));

            Optional<Usuario> usuario = usuarioRepository.buscarPorDocumento(documentoUsuario);
            evento.setNombreUsuario(usuario.map(Usuario::nombreCompleto).orElse("Desconocido"));
            evento.setRolUsuario(usuario.map(u -> u.getRol().name()).orElse("DESCONOCIDO"));

            agregarDatosPeticion(evento);
            logRepository.guardar(modulo, evento);
        } catch (RuntimeException e) {
            log.warn("No se pudo guardar el log {} {} ({}): {}", modulo, accion, referencia, e.getMessage());
        }
    }

    @Override
    public List<LogResponseDTO> listar(Integer documentoAdmin,
                                       LocalDate desde,
                                       LocalDate hasta,
                                       ModuloLog modulo) {
        validarAdmin(documentoAdmin);

        if (desde.isAfter(hasta)) {
            throw new ReglaNegocioException(45001, "La fecha inicial no puede ser posterior a la final.");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) > MAX_DIAS_CONSULTA) {
            throw new ReglaNegocioException(45001, "El rango de fechas no puede superar un año.");
        }

        // Cada cubeta trae varios eventos: se aplanan y se ordenan del mas reciente al mas antiguo.
        return logRepository.buscar(desde, hasta, modulo).stream()
                .flatMap(cubeta -> cubeta.getEventos().stream().map(e -> aDTO(cubeta, e)))
                .sorted(Comparator.comparing(LogResponseDTO::getFecha).reversed())
                .toList();
    }

    private void validarAdmin(Integer documentoAdmin) {
        boolean esAdmin = usuarioRepository.buscarPorDocumento(documentoAdmin)
                .filter(u -> u.getRol() == Rol.ADMIN && u.getEstado() == EstadoUsuario.ACTIVO)
                .isPresent();
        if (!esAdmin) {
            throw new NoAutorizadoException("Solo el administrador puede consultar los logs.");
        }
    }

    private LogResponseDTO aDTO(LogBucket cubeta, LogEvento evento) {
        LogResponseDTO dto = mm.map(evento, LogResponseDTO.class);
        dto.setModulo(cubeta.getModulo());
        dto.setDetalle(evento.getDetalle());
        return dto;
    }

    // IP, navegador y endpoint de la peticion que esta en curso (si la hay).
    private void agregarDatosPeticion(LogEvento evento) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos)) {
            return;
        }
        HttpServletRequest peticion = atributos.getRequest();
        String reenviada = peticion.getHeader("X-Forwarded-For");
        evento.setIp(reenviada != null && !reenviada.isBlank()
                ? reenviada.split(",")[0].trim()
                : peticion.getRemoteAddr());
        evento.setUserAgent(peticion.getHeader("User-Agent"));
        evento.setEndpoint(peticion.getMethod() + " " + peticion.getRequestURI());
    }

    // Deja el detalle solo con tipos simples (texto, numeros, booleanos, mapas y listas)
    // para que se guarde igual en Mongo y llegue legible al front.
    private Map<String, Object> normalizar(Map<String, Object> detalle) {
        Map<String, Object> limpio = new LinkedHashMap<>();
        if (detalle == null) return limpio;
        detalle.forEach((clave, valor) -> {
            if (valor != null) limpio.put(clave, valorSimple(valor));
        });
        return limpio;
    }

    @SuppressWarnings("unchecked")
    private Object valorSimple(Object valor) {
        if (valor instanceof Map<?, ?> mapa) return normalizar((Map<String, Object>) mapa);
        if (valor instanceof Collection<?> lista) return lista.stream().map(this::valorSimple).toList();
        if (valor instanceof Enum<?> e) return e.name();
        if (valor instanceof BigDecimal numero) return numero.doubleValue();
        if (valor instanceof TemporalAccessor) return valor.toString();   // fechas en ISO
        return valor;
    }
}
