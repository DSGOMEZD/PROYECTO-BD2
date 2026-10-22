package com.basesdatos2.backendBD2.Modelo.dto;

import com.basesdatos2.backendBD2.Modelo.emun.EstadoPago;
import com.basesdatos2.backendBD2.Modelo.emun.MetodoPago;
import java.time.LocalDate;

// fechaPago y metodoPago van en null mientras el pago no este PAGADO.
public record PagoResponseDTO(
        Integer idPago,
        Integer monto,
        LocalDate fechaVencimiento,
        LocalDate fechaPago,
        EstadoPago estado,
        MetodoPago metodoPago,
        Integer idContrato
) {}
