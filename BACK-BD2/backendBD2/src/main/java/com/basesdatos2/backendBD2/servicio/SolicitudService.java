package com.basesdatos2.backendBD2.servicio;

import com.basesdatos2.backendBD2.modelo.dto.ContratoResponseDTO;
import com.basesdatos2.backendBD2.modelo.dto.SolicitudRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.SolicitudResponseDTO;

import java.util.List;

public interface SolicitudService {
    SolicitudResponseDTO crear(
            Integer documentoArrendatario,
            SolicitudRequestDTO dto
    );

    List<SolicitudResponseDTO> listarMias(Integer documentoArrendatario);

    List<SolicitudResponseDTO> listarRecibidas(Integer documentoArrendador);
    ContratoResponseDTO aprobar(Integer idSolicitud, Integer documentoArrendador);
    void rechazar(Integer idSolicitud, Integer documentoArrendador);
}
