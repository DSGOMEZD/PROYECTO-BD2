package com.basesdatos2.backendBD2.Modelo.dto;

import com.basesdatos2.backendBD2.Modelo.emun.EstadoContrato;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// accion: FINALIZADO o CANCELADO. kilometrajeFinal solo se usa al finalizar.
public record CerrarContratoRequestDTO(
        @NotNull EstadoContrato accion,
        @PositiveOrZero Integer kilometrajeFinal
) {}
