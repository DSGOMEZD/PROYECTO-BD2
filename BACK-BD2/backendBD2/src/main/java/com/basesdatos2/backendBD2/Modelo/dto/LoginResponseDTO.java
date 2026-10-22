package com.basesdatos2.backendBD2.Modelo.dto;

public record LoginResponseDTO(
        String token,
        UsuarioResponseDTO usuario
) {}
