package com.basesdatos2.backendBD2.servicio;

import com.basesdatos2.backendBD2.modelo.dto.ContratoResponseDTO;
import java.util.List;

public interface ContratoService {
    List<ContratoResponseDTO> listarMios(Integer documentoArrendatario);
    List<ContratoResponseDTO> listarComoArrendador(Integer documentoArrendador);
    void cancelar(Integer idContrato, Integer documento);
    void finalizar(Integer idContrato, Integer documentoArrendador, Integer kilometrajeFinal);
}