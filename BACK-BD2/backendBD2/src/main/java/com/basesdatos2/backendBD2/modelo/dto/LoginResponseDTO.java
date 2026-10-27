package com.basesdatos2.backendBD2.modelo.dto;

public record LoginResponseDTO(
        String token,
        UsuarioResponseDTO usuario
) {}
