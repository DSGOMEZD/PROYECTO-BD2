package com.basesdatos2.backendBD2.servicio.implementacion;

import com.basesdatos2.backendBD2.modelo.dto.ContratoResponseDTO;
import com.basesdatos2.backendBD2.modelo.dto.SolicitudRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.SolicitudResponseDTO;
import com.basesdatos2.backendBD2.repositorio.SolicitudRepository;
import com.basesdatos2.backendBD2.servicio.SolicitudService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SolicitudServiceImpl implements SolicitudService {
    private final SolicitudRepository solicitudRepository;

    public SolicitudServiceImpl(SolicitudRepository solicitudRepository) {
        this.solicitudRepository = solicitudRepository;
    }

    @Override
    public SolicitudResponseDTO crear(
            Integer documentoArrendatario,
            SolicitudRequestDTO dto
    ) {
        return solicitudRepository.crear(documentoArrendatario, dto);
    }

    @Override
    public List<SolicitudResponseDTO> listarMias(Integer documentoArrendatario) {
        return solicitudRepository.listarMias(documentoArrendatario);
    }

    @Override
    public List<SolicitudResponseDTO> listarRecibidas(Integer documentoArrendador) {
        return solicitudRepository.listarRecibidas(documentoArrendador);
    }

    @Override
    public ContratoResponseDTO aprobar(
            Integer idSolicitud,
            Integer documentoArrendador,
            Integer deposito
    ) {
        return solicitudRepository.aprobar(idSolicitud, documentoArrendador, deposito);
    }


    @Override
    public void rechazar(Integer idSolicitud, Integer documentoArrendador) {
        solicitudRepository.rechazar(idSolicitud, documentoArrendador);
    }

    @Override
    public void cancelar(Integer idSolicitud, Integer documentoArrendatario) {
        solicitudRepository.cancelar(idSolicitud, documentoArrendatario);
    }
}
