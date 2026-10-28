package com.basesdatos2.backendBD2.modelo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AprobarSolicitudRequestDTO(
        @NotNull @PositiveOrZero Integer deposito
) {}