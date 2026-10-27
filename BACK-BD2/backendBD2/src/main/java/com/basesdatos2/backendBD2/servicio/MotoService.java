package com.basesdatos2.backendBD2.servicio;

import java.time.LocalDate;
import java.util.List;

import com.basesdatos2.backendBD2.modelo.dto.MotocicletaRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.MotocicletaResponseDTO;
import com.basesdatos2.backendBD2.modelo.emun.EstadoMotocicleta;

public interface MotoService {
    List<MotocicletaResponseDTO> listarDisponibles(
            LocalDate fechaInicio,
            LocalDate fechaFin
    );

    Integer registrar(Integer documentoArrendador, MotocicletaRequestDTO dto);

    List<MotocicletaResponseDTO> listarMias(Integer documentoArrendador);

    void cambiarEstado(
            String placa,
            Integer documentoArrendador,
            EstadoMotocicleta estado
    );

    void actualizar(
            String placa,
            Integer documentoArrendador,
            MotocicletaRequestDTO dto
    );
}
