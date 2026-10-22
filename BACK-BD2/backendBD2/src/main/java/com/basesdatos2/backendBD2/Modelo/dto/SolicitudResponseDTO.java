package com.basesdatos2.backendBD2.Modelo.dto;

import com.basesdatos2.backendBD2.Modelo.emun.EstadoSolicitud;
import java.time.LocalDate;

// fechaInicio y fechaFin vienen del contrato asociado a la solicitud.
public record SolicitudResponseDTO(
        Integer idSolicitud,
        LocalDate fechaSolicitud,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String mensaje,
        EstadoSolicitud estado,
        Integer documentoArrendatario,
        String nombreArrendatario,
        Integer documentoArrendador,
        MotoResumenDTO moto
) {}
