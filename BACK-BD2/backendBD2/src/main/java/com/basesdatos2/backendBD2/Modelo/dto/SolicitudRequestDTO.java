package com.basesdatos2.backendBD2.Modelo.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

// El arrendatario se toma del usuario logueado, no del body.
public record SolicitudRequestDTO(
        @NotBlank @Size(max = 10) String placa,
        @NotNull @FutureOrPresent LocalDate fechaInicio,
        @NotNull @FutureOrPresent LocalDate fechaFin,
        @Size(max = 200) String mensaje
) {}
