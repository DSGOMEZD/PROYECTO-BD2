package com.basesdatos2.backendBD2.Modelo.dto;

// Una fila de sp_reporte_ingresos_arrendador (una por moto).
public record ReporteIngresosDTO(
        String placa,
        String marca,
        String modelo,
        Integer contratos,
        Long ingresos,
        Long costosMantenimiento,
        Long utilidad
) {}
