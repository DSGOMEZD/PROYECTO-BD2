package com.basesdatos2.backendBD2.Modelo.dto;

import com.basesdatos2.backendBD2.Modelo.emun.EstadoSolicitud;
import jakarta.validation.constraints.NotNull;

// decision: APROBADA o RECHAZADA.
public record ResponderSolicitudRequestDTO(
        @NotNull EstadoSolicitud decision
) {}
