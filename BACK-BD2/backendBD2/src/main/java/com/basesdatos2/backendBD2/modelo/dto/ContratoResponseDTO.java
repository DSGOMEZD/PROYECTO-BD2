package com.basesdatos2.backendBD2.modelo.dto;

import com.basesdatos2.backendBD2.modelo.emun.EstadoContrato;
import java.time.LocalDate;

public record ContratoResponseDTO(
        Integer idContrato,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Integer valorTotal,
        Integer deposito,
        EstadoContrato estado,
        Integer idSolicitud,
        Integer documentoArrendatario,
        String nombreArrendatario,
        Integer documentoArrendador,
        String nombreArrendador,
        MotoResumenDTO moto
) {}
