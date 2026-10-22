package com.basesdatos2.backendBD2.Modelo.dto;

import com.basesdatos2.backendBD2.Modelo.emun.Rol;
import com.basesdatos2.backendBD2.Modelo.emun.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RegistroRequestDTO(
        @NotNull TipoDocumento tipoDocumento,
        @NotNull @Positive Integer numeroDocumento,
        @NotBlank @Size(max = 50) String primerNombre,
        @Size(max = 50) String segundoNombre,
        @NotBlank @Size(max = 50) String primerApellido,
        @Size(max = 50) String segundoApellido,
        @NotBlank @Email @Size(max = 50) String correo,
        @NotNull @Positive Integer telefono,
        @NotBlank @Size(min = 8, max = 72) String contrasena,
        @NotNull Rol rol
) {}
