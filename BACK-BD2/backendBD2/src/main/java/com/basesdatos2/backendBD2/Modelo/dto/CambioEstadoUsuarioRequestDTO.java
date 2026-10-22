package com.basesdatos2.backendBD2.Modelo.dto;

import com.basesdatos2.backendBD2.Modelo.emun.EstadoUsuario;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoUsuarioRequestDTO(
        @NotNull EstadoUsuario estado
) {}
