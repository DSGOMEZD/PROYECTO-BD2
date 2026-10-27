package com.basesdatos2.backendBD2.modelo;

import com.basesdatos2.backendBD2.modelo.emun.EstadoPago;
import com.basesdatos2.backendBD2.modelo.emun.MetodoPago;
import java.time.LocalDate;

// Tabla PAGO
public class Pago {

    private Integer idPago;
    private LocalDate fechaPago;          // mientras esta PENDIENTE guarda la fecha de generacion
    private Integer monto;
    private LocalDate fechaVencimiento;
    private EstadoPago estado;
    private MetodoPago metodoPago;        // POR_DEFINIR mientras esta PENDIENTE
    private Integer idContrato;
    private Integer documentoArrendatario;  // columna Numero_documento
    private Integer documentoArrendador;    // columna Numero_documento1

    public Pago() {}

    public Pago(Integer idPago, LocalDate fechaPago, Integer monto, LocalDate fechaVencimiento,
                EstadoPago estado, MetodoPago metodoPago, Integer idContrato,
                Integer documentoArrendatario, Integer documentoArrendador) {
        this.idPago = idPago;
        this.fechaPago = fechaPago;
        this.monto = monto;
        this.fechaVencimiento = fechaVencimiento;
        this.estado = estado;
        this.metodoPago = metodoPago;
        this.idContrato = idContrato;
        this.documentoArrendatario = documentoArrendatario;
        this.documentoArrendador = documentoArrendador;
    }

    public Integer getIdPago() { return idPago; }
    public void setIdPago(Integer idPago) { this.idPago = idPago; }

    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }

    public Integer getMonto() { return monto; }
    public void setMonto(Integer monto) { this.monto = monto; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public EstadoPago getEstado() { return estado; }
    public void setEstado(EstadoPago estado) { this.estado = estado; }

    public MetodoPago getMetodoPago() { return metodoPago; }
    public void setMetodoPago(MetodoPago metodoPago) { this.metodoPago = metodoPago; }

    public Integer getIdContrato() { return idContrato; }
    public void setIdContrato(Integer idContrato) { this.idContrato = idContrato; }

    public Integer getDocumentoArrendatario() { return documentoArrendatario; }
    public void setDocumentoArrendatario(Integer documentoArrendatario) { this.documentoArrendatario = documentoArrendatario; }

    public Integer getDocumentoArrendador() { return documentoArrendador; }
    public void setDocumentoArrendador(Integer documentoArrendador) { this.documentoArrendador = documentoArrendador; }
}
