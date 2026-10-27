package com.basesdatos2.backendBD2.modelo.dto;

import com.basesdatos2.backendBD2.modelo.emun.EstadoUsuario;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoUsuarioRequestDTO(
        @NotNull EstadoUsuario estado
) {}
