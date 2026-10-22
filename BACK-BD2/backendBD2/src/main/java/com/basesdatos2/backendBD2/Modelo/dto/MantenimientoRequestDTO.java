package com.basesdatos2.backendBD2.Modelo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record MantenimientoRequestDTO(
        @NotBlank @Size(max = 10) String placa,
        @NotNull @PastOrPresent LocalDate fechaMantenimiento,
        @NotBlank @Size(max = 50) String tipoMantenimiento,
        @NotNull @PositiveOrZero Integer kilometrajeAlMomento,
        @NotBlank @Size(max = 500) String descripcion,
        @NotNull @PositiveOrZero Integer costo
) {}
