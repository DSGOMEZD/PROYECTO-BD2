package com.basesdatos2.backendBD2.servicio.implementacion;

import com.basesdatos2.backendBD2.modelo.dto.ContratoResponseDTO;
import com.basesdatos2.backendBD2.repositorio.ContratoRepository;
import com.basesdatos2.backendBD2.servicio.ContratoService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContratoServiceImpl implements ContratoService {
    private final ContratoRepository contratoRepository;

    public ContratoServiceImpl(ContratoRepository contratoRepository) {
        this.contratoRepository = contratoRepository;
    }

    @Override
    public List<ContratoResponseDTO> listarMios(Integer documentoArrendatario) {
        return contratoRepository.listarMios(documentoArrendatario);
    }

    @Override
    public List<ContratoResponseDTO> listarComoArrendador(Integer documentoArrendador) {
        return contratoRepository.listarComoArrendador(documentoArrendador);
    }

    @Override
    public void cancelar(Integer idContrato, Integer documento) {
        contratoRepository.cancelar(idContrato, documento);
    }

    @Override
    public void finalizar(
            Integer idContrato,
            Integer documentoArrendador,
            Integer kilometrajeFinal
    ) {
        contratoRepository.finalizar(
                idContrato,
                documentoArrendador,
                kilometrajeFinal
        );
    }
}