package com.basesdatos2.backendBD2.Modelo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank @Email String correo,
        @NotBlank String contrasena
) {}
