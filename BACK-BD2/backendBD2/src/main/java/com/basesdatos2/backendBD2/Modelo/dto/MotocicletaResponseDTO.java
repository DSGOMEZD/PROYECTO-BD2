package com.basesdatos2.backendBD2.Modelo.dto;

import com.basesdatos2.backendBD2.Modelo.emun.EstadoMotocicleta;
import java.math.BigDecimal;

// valorEstimado solo viene lleno en el catalogo de motos disponibles por fechas.
public record MotocicletaResponseDTO(
        String placa,
        String marca,
        String modelo,
        Integer anio,
        BigDecimal cilindraje,
        Integer kilometraje,
        Integer precioDia,
        EstadoMotocicleta estado,
        Integer documentoArrendador,
        String nombreArrendador,
        SeguroResponseDTO seguro,
        Integer valorEstimado
) {}
