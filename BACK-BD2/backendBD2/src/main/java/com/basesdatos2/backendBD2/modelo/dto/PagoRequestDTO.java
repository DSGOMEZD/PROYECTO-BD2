package com.basesdatos2.backendBD2.modelo.dto;

import com.basesdatos2.backendBD2.modelo.emun.MetodoPago;
import jakarta.validation.constraints.NotNull;

// El pago ya existe (se genera al aprobar la solicitud); aqui solo se paga.
public record PagoRequestDTO(
        @NotNull MetodoPago metodoPago
) {}
