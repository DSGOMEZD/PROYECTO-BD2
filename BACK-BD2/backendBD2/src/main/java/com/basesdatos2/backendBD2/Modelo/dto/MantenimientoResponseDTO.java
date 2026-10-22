package com.basesdatos2.backendBD2.Modelo.dto;

import java.time.LocalDate;

public record MantenimientoResponseDTO(
        Integer idMantenimiento,
        LocalDate fechaMantenimiento,
        String tipoMantenimiento,
        Integer kilometrajeAlMomento,
        String descripcion,
        Integer costo,
        String placa
) {}
