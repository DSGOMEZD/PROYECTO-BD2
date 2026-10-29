package com.basesdatos2.backendBD2.servicio.implementacion;

import com.basesdatos2.backendBD2.modelo.Contrato;
import com.basesdatos2.backendBD2.modelo.Motocicleta;
import com.basesdatos2.backendBD2.modelo.Solicitud;
import com.basesdatos2.backendBD2.modelo.Usuario;
import com.basesdatos2.backendBD2.modelo.dto.ContratoResponseDTO;
import com.basesdatos2.backendBD2.modelo.dto.MotoResumenDTO;
import com.basesdatos2.backendBD2.modelo.dto.SolicitudRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.SolicitudResponseDTO;
import com.basesdatos2.backendBD2.modelo.excepcion.RecursoNoEncontradoException;
import com.basesdatos2.backendBD2.repositorio.ContratoRepository;
import com.basesdatos2.backendBD2.repositorio.MotoRepository;
import com.basesdatos2.backendBD2.repositorio.SolicitudRepository;
import com.basesdatos2.backendBD2.repositorio.UsuarioRepository;
import com.basesdatos2.backendBD2.servicio.ContratoService;
import com.basesdatos2.backendBD2.servicio.SolicitudService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SolicitudServiceImpl implements SolicitudService {
    private final SolicitudRepository solicitudRepository;
    private final ContratoRepository contratoRepository;
    private final MotoRepository motoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ContratoService contratoService;
    private final ModelMapper mm;

    public SolicitudServiceImpl(SolicitudRepository solicitudRepository,
                                ContratoRepository contratoRepository,
                                MotoRepository motoRepository,
                                UsuarioRepository usuarioRepository,
                                ContratoService contratoService,
                                ModelMapper mm) {
        this.solicitudRepository = solicitudRepository;
        this.contratoRepository = contratoRepository;
        this.motoRepository = motoRepository;
        this.usuarioRepository = usuarioRepository;
        this.contratoService = contratoService;
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
        return aDTO(buscar(idSolicitud));
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

    @Override
    public ContratoResponseDTO aprobar(
            Integer idSolicitud,
            Integer documentoArrendador,
            Integer deposito
    ) {
        solicitudRepository.aprobar(idSolicitud, documentoArrendador, deposito);
        return contratoService.buscarPorId(buscar(idSolicitud).getIdContrato());
    }

    @Override
    public void rechazar(Integer idSolicitud, Integer documentoArrendador) {
        solicitudRepository.rechazar(idSolicitud, documentoArrendador);
    }

    @Override
    public void cancelar(Integer idSolicitud, Integer documentoArrendatario) {
        solicitudRepository.cancelar(idSolicitud, documentoArrendatario);
    }

    private Solicitud buscar(Integer idSolicitud) {
        return solicitudRepository.buscarPorId(idSolicitud)
                .orElseThrow(() -> new RecursoNoEncontradoException("La solicitud no existe."));
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
