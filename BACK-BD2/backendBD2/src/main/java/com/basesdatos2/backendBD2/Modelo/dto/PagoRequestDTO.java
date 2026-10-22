package com.basesdatos2.backendBD2.Modelo.dto;

import com.basesdatos2.backendBD2.Modelo.emun.MetodoPago;
import jakarta.validation.constraints.NotNull;

// El pago ya existe (se genera al aprobar la solicitud); aqui solo se paga.
public record PagoRequestDTO(
        @NotNull MetodoPago metodoPago
) {}
