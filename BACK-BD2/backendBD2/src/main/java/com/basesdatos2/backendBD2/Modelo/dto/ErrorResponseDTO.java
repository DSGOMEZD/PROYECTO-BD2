package com.basesdatos2.backendBD2.Modelo.dto;

// codigo: MYSQL_ERRNO de los SIGNAL de la base de datos (45001..45010) u otro.
public record ErrorResponseDTO(
        Integer codigo,
        String mensaje
) {}
