package com.basesdatos2.backendBD2.modelo.dto;

import com.basesdatos2.backendBD2.modelo.emun.EstadoMotocicleta;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequestDTO(
        @NotNull EstadoMotocicleta estado
) {}