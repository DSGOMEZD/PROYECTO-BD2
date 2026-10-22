package com.basesdatos2.backendBD2.Modelo.dto;

import com.basesdatos2.backendBD2.Modelo.emun.EstadoPoliza;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

// El id lo asigna la base de datos.
public record SeguroRequestDTO(
        @NotNull @Positive Integer numeroPoliza,
        @NotBlank @Size(max = 50) String aseguradora,
        @NotNull LocalDate fechaInicioVigencia,
        @NotNull EstadoPoliza estadoPoliza
) {}
