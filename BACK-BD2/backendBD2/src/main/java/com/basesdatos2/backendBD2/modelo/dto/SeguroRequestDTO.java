package com.basesdatos2.backendBD2.modelo.dto;

import com.basesdatos2.backendBD2.modelo.emun.EstadoPoliza;
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
