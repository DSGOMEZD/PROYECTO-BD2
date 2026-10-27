package com.basesdatos2.backendBD2.modelo.dto;

// Datos basicos de la moto que se muestran dentro de solicitudes y contratos.
public record MotoResumenDTO(
        String placa,
        String marca,
        String modelo,
        Integer precioDia
) {}
