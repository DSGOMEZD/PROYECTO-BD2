package com.basesdatos2.backendBD2.modelo.dto;

import com.basesdatos2.backendBD2.modelo.emun.EstadoPago;
import com.basesdatos2.backendBD2.modelo.emun.MetodoPago;
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
