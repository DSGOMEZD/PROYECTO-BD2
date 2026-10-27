package com.basesdatos2.backendBD2.modelo.dto;

import com.basesdatos2.backendBD2.modelo.emun.EstadoPoliza;
import java.time.LocalDate;

public record SeguroResponseDTO(
        Integer idSeguro,
        Integer numeroPoliza,
        String aseguradora,
        LocalDate fechaInicioVigencia,
        EstadoPoliza estadoPoliza
) {}
