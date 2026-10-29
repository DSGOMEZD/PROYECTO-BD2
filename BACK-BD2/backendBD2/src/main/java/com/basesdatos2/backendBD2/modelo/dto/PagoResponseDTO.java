package com.basesdatos2.backendBD2.modelo.dto;

import com.basesdatos2.backendBD2.modelo.emun.EstadoPago;
import com.basesdatos2.backendBD2.modelo.emun.MetodoPago;
import java.time.LocalDate;

// Mientras el pago esta PENDIENTE, fechaPago es la fecha en que se genero
// y metodoPago vale POR_DEFINIR.
public class PagoResponseDTO {

    private Integer idPago;
    private Integer monto;
    private LocalDate fechaVencimiento;
    private LocalDate fechaPago;
    private EstadoPago estado;
    private MetodoPago metodoPago;
    private Integer idContrato;

    public PagoResponseDTO() {}

    public PagoResponseDTO(Integer idPago, Integer monto, LocalDate fechaVencimiento,
                           LocalDate fechaPago, EstadoPago estado, MetodoPago metodoPago,
                           Integer idContrato) {
        this.idPago = idPago;
        this.monto = monto;
        this.fechaVencimiento = fechaVencimiento;
        this.fechaPago = fechaPago;
        this.estado = estado;
        this.metodoPago = metodoPago;
        this.idContrato = idContrato;
    }

    public Integer getIdPago() { return idPago; }
    public void setIdPago(Integer idPago) { this.idPago = idPago; }

    public Integer getMonto() { return monto; }
    public void setMonto(Integer monto) { this.monto = monto; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }

    public EstadoPago getEstado() { return estado; }
    public void setEstado(EstadoPago estado) { this.estado = estado; }

    public MetodoPago getMetodoPago() { return metodoPago; }
    public void setMetodoPago(MetodoPago metodoPago) { this.metodoPago = metodoPago; }

    public Integer getIdContrato() { return idContrato; }
    public void setIdContrato(Integer idContrato) { this.idContrato = idContrato; }
}
