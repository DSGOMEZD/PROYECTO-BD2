package com.basesdatos2.backendBD2.modelo.dto;

import com.basesdatos2.backendBD2.modelo.emun.EstadoSolicitud;
import jakarta.validation.constraints.NotNull;

// decision: APROBADA o RECHAZADA.
public record ResponderSolicitudRequestDTO(
        @NotNull EstadoSolicitud decision
) {}
