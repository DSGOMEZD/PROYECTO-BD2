package com.basesdatos2.backendBD2.modelo.dto;

import com.basesdatos2.backendBD2.modelo.emun.MetodoPago;
import jakarta.validation.constraints.NotNull;

public record RegistrarPagoRequestDTO(
        @NotNull MetodoPago metodoPago
) {}