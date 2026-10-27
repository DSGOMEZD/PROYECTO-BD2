package com.basesdatos2.backendBD2.servicio.implementacion;

import java.time.LocalDate;
import java.util.List;

import com.basesdatos2.backendBD2.modelo.dto.MotocicletaRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.MotocicletaResponseDTO;
import com.basesdatos2.backendBD2.modelo.emun.EstadoMotocicleta;
import com.basesdatos2.backendBD2.repositorio.MotoRepository;
import com.basesdatos2.backendBD2.servicio.MotoService;
import org.springframework.stereotype.Service;

@Service
public class MotoServiceImpl implements MotoService {

    private final MotoRepository motoRepository;

    public MotoServiceImpl(MotoRepository motoRepository) {
        this.motoRepository = motoRepository;
    }

    @Override
    public List<MotocicletaResponseDTO> listarDisponibles(
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
        return motoRepository.listarDisponibles(fechaInicio, fechaFin);
    }

    @Override
    public Integer registrar(Integer documentoArrendador, MotocicletaRequestDTO dto) {
        return motoRepository.registrar(documentoArrendador, dto);
    }

    @Override
    public List<MotocicletaResponseDTO> listarMias(Integer documentoArrendador) {
        return motoRepository.listarMias(documentoArrendador);
    }

    @Override
    public void cambiarEstado(
            String placa,
            Integer documentoArrendador,
            EstadoMotocicleta estado) {
        motoRepository.cambiarEstado(placa, documentoArrendador, estado);
    }

}
