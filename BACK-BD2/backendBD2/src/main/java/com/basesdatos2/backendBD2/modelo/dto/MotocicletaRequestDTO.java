package com.basesdatos2.backendBD2.modelo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

// Lo que el arrendador envia para publicar una moto (con su seguro).
public record MotocicletaRequestDTO(
        @NotBlank @Size(max = 10) String placa,
        @NotBlank @Size(max = 50) String marca,
        @NotBlank @Size(max = 50) String modelo,
        @Size(max = 30) String color,
        @Size(max = 200) String descripcion,
        @NotNull @Min(1950) Integer anio,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal cilindraje,
        @NotNull @PositiveOrZero Integer kilometraje,
        @NotNull @Positive Integer precioDia,
        @NotNull @Valid SeguroRequestDTO seguro
) {}