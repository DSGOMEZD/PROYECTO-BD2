package com.basesdatos2.backendBD2.servicio.implementacion;

import com.basesdatos2.backendBD2.modelo.Contrato;
import com.basesdatos2.backendBD2.modelo.Motocicleta;
import com.basesdatos2.backendBD2.modelo.Solicitud;
import com.basesdatos2.backendBD2.modelo.Usuario;
import com.basesdatos2.backendBD2.modelo.dto.ContratoResponseDTO;
import com.basesdatos2.backendBD2.modelo.dto.MotoResumenDTO;
import com.basesdatos2.backendBD2.modelo.dto.SolicitudRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.SolicitudResponseDTO;
import com.basesdatos2.backendBD2.modelo.emun.ModuloLog;
import com.basesdatos2.backendBD2.modelo.excepcion.RecursoNoEncontradoException;
import com.basesdatos2.backendBD2.repositorio.ContratoRepository;
import com.basesdatos2.backendBD2.repositorio.MotoRepository;
import com.basesdatos2.backendBD2.repositorio.SolicitudRepository;
import com.basesdatos2.backendBD2.repositorio.UsuarioRepository;
import com.basesdatos2.backendBD2.servicio.ContratoService;
import com.basesdatos2.backendBD2.servicio.LogService;
import com.basesdatos2.backendBD2.servicio.SolicitudService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SolicitudServiceImpl implements SolicitudService {
    private final SolicitudRepository solicitudRepository;
    private final ContratoRepository contratoRepository;
    private final MotoRepository motoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ContratoService contratoService;
    private final LogService logService;
    private final ModelMapper mm;

    public SolicitudServiceImpl(SolicitudRepository solicitudRepository,
                                ContratoRepository contratoRepository,
                                MotoRepository motoRepository,
                                UsuarioRepository usuarioRepository,
                                ContratoService contratoService,
                                LogService logService,
                                ModelMapper mm) {
        this.solicitudRepository = solicitudRepository;
        this.contratoRepository = contratoRepository;
        this.motoRepository = motoRepository;
        this.usuarioRepository = usuarioRepository;
        this.contratoService = contratoService;
        this.logService = logService;
        this.mm = mm;
    }

    // La solicitud guarda placa y mensaje; su contrato PENDIENTE guarda las fechas.
    @Override
    public SolicitudResponseDTO crear(
            Integer documentoArrendatario,
            SolicitudRequestDTO dto
    ) {
        Solicitud solicitud = mm.map(dto, Solicitud.class);
        solicitud.setDocumentoArrendatario(documentoArrendatario);
        Contrato contrato = mm.map(dto, Contrato.class);

        Integer idSolicitud = solicitudRepository.crear(solicitud, contrato);
        Solicitud creada = buscar(idSolicitud);
        SolicitudResponseDTO respuesta = aDTO(creada);

        logService.registrar(ModuloLog.SOLICITUD, "CREADA", documentoArrendatario, "Solicitud #" + idSolicitud,
                "Solicitó la moto " + creada.getPlaca() + " del " + LogService.fecha(respuesta.getFechaInicio())
                        + " al " + LogService.fecha(respuesta.getFechaFin()) + ".",
                detalleSolicitud(creada, respuesta));
        return respuesta;
    }

    @Override
    public List<SolicitudResponseDTO> listarMias(Integer documentoArrendatario) {
        return solicitudRepository.listarMias(documentoArrendatario).stream()
                .map(this::aDTO)
                .toList();
    }

    @Override
    public List<SolicitudResponseDTO> listarRecibidas(Integer documentoArrendador) {
        return solicitudRepository.listarRecibidas(documentoArrendador).stream()
                .map(this::aDTO)
                .toList();
    }

    // Aprobar la solicitud genera el contrato: se registran los dos eventos.
    @Override
    public ContratoResponseDTO aprobar(
            Integer idSolicitud,
            Integer documentoArrendador,
            Integer deposito
    ) {
        solicitudRepository.aprobar(idSolicitud, documentoArrendador, deposito);
        Solicitud aprobada = buscar(idSolicitud);
        ContratoResponseDTO contrato = contratoService.buscarPorId(aprobada.getIdContrato());

        Map<String, Object> detalle = detalleSolicitud(aprobada, aDTO(aprobada));
        detalle.put("deposito", deposito);
        detalle.put("valorTotal", contrato.getValorTotal());
        logService.registrar(ModuloLog.SOLICITUD, "APROBADA", documentoArrendador, "Solicitud #" + idSolicitud,
                "Aprobó la solicitud de " + contrato.getNombreArrendatario() + " con depósito de "
                        + LogService.pesos(deposito) + ".",
                detalle);

        contratoService.registrarLogGenerado(contrato, documentoArrendador);
        return contrato;
    }

    @Override
    public void rechazar(Integer idSolicitud, Integer documentoArrendador) {
        SolicitudResponseDTO antes = aDTO(buscar(idSolicitud));

        solicitudRepository.rechazar(idSolicitud, documentoArrendador);

        Solicitud rechazada = buscar(idSolicitud);
        Map<String, Object> detalle = detalleSolicitud(rechazada, antes);
        detalle.put("estadoAnterior", antes.getEstado());
        logService.registrar(ModuloLog.SOLICITUD, "RECHAZADA", documentoArrendador, "Solicitud #" + idSolicitud,
                "Rechazó la solicitud de " + antes.getNombreArrendatario() + " para la moto "
                        + rechazada.getPlaca() + ".",
                detalle);
    }

    @Override
    public void cancelar(Integer idSolicitud, Integer documentoArrendatario) {
        SolicitudResponseDTO antes = aDTO(buscar(idSolicitud));

        solicitudRepository.cancelar(idSolicitud, documentoArrendatario);

        Solicitud cancelada = buscar(idSolicitud);
        Map<String, Object> detalle = detalleSolicitud(cancelada, antes);
        detalle.put("estadoAnterior", antes.getEstado());
        logService.registrar(ModuloLog.SOLICITUD, "CANCELADA", documentoArrendatario, "Solicitud #" + idSolicitud,
                "Canceló su solicitud para la moto " + cancelada.getPlaca() + ".",
                detalle);
    }

    private Solicitud buscar(Integer idSolicitud) {
        return solicitudRepository.buscarPorId(idSolicitud)
                .orElseThrow(() -> new RecursoNoEncontradoException("La solicitud no existe."));
    }

    // Snapshot de la solicitud (estado actual) con fechas, moto y las dos partes.
    private Map<String, Object> detalleSolicitud(Solicitud solicitud, SolicitudResponseDTO dto) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("idSolicitud", solicitud.getIdSolicitud());
        detalle.put("idContrato", solicitud.getIdContrato());
        detalle.put("estado", solicitud.getEstado());
        detalle.put("fechaSolicitud", solicitud.getFechaSolicitud());
        detalle.put("fechaInicio", dto.getFechaInicio());
        detalle.put("fechaFin", dto.getFechaFin());
        int dias = (int) ChronoUnit.DAYS.between(dto.getFechaInicio(), dto.getFechaFin()) + 1;
        detalle.put("dias", dias);
        detalle.put("mensaje", solicitud.getMensaje());
        detalle.put("documentoArrendatario", solicitud.getDocumentoArrendatario());
        detalle.put("nombreArrendatario", dto.getNombreArrendatario());
        detalle.put("documentoArrendador", solicitud.getDocumentoArrendador());
        detalle.put("nombreArrendador", usuarioRepository.buscarPorDocumento(solicitud.getDocumentoArrendador())
                .map(Usuario::nombreCompleto)
                .orElse(null));

        MotoResumenDTO moto = dto.getMoto();
        Map<String, Object> datosMoto = new LinkedHashMap<>();
        datosMoto.put("placa", moto.getPlaca());
        datosMoto.put("marca", moto.getMarca());
        datosMoto.put("modelo", moto.getModelo());
        datosMoto.put("precioDia", moto.getPrecioDia());
        detalle.put("moto", datosMoto);
        detalle.put("valorEstimado", moto.getPrecioDia() * dias);
        return detalle;
    }

    // Une la solicitud con las fechas de su contrato, la moto y el nombre del arrendatario.
    private SolicitudResponseDTO aDTO(Solicitud solicitud) {
        Contrato contrato = contratoRepository.buscarPorId(solicitud.getIdContrato())
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato de la solicitud no existe."));
        Motocicleta moto = motoRepository.buscar(solicitud.getPlaca(), solicitud.getDocumentoArrendador())
                .orElseThrow(() -> new RecursoNoEncontradoException("La motocicleta de la solicitud no existe."));

        SolicitudResponseDTO dto = mm.map(solicitud, SolicitudResponseDTO.class);
        dto.setFechaInicio(contrato.getFechaInicio());
        dto.setFechaFin(contrato.getFechaFin());
        dto.setNombreArrendatario(usuarioRepository.buscarPorDocumento(solicitud.getDocumentoArrendatario())
                .map(Usuario::nombreCompleto)
                .orElse(null));
        dto.setMoto(aResumen(moto));
        return dto;
    }

    private MotoResumenDTO aResumen(Motocicleta moto) {
        MotoResumenDTO resumen = mm.map(moto, MotoResumenDTO.class);
        resumen.setPrecioDia(moto.getPrecioAlquiler());
        return resumen;
    }
}
