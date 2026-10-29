package com.basesdatos2.backendBD2.modelo.dto;

import com.basesdatos2.backendBD2.modelo.emun.EstadoContrato;
import java.time.LocalDate;

public class ContratoResponseDTO {

    private Integer idContrato;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Integer valorTotal;
    private Integer deposito;
    private EstadoContrato estado;
    private Integer idSolicitud;
    private Integer documentoArrendatario;
    private String nombreArrendatario;
    private Integer documentoArrendador;
    private String nombreArrendador;
    private MotoResumenDTO moto;

    public ContratoResponseDTO() {}

    public ContratoResponseDTO(Integer idContrato, LocalDate fechaInicio, LocalDate fechaFin,
                               Integer valorTotal, Integer deposito, EstadoContrato estado,
                               Integer idSolicitud, Integer documentoArrendatario,
                               String nombreArrendatario, Integer documentoArrendador,
                               String nombreArrendador, MotoResumenDTO moto) {
        this.idContrato = idContrato;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.valorTotal = valorTotal;
        this.deposito = deposito;
        this.estado = estado;
        this.idSolicitud = idSolicitud;
        this.documentoArrendatario = documentoArrendatario;
        this.nombreArrendatario = nombreArrendatario;
        this.documentoArrendador = documentoArrendador;
        this.nombreArrendador = nombreArrendador;
        this.moto = moto;
    }

    public Integer getIdContrato() { return idContrato; }
    public void setIdContrato(Integer idContrato) { this.idContrato = idContrato; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public Integer getValorTotal() { return valorTotal; }
    public void setValorTotal(Integer valorTotal) { this.valorTotal = valorTotal; }

    public Integer getDeposito() { return deposito; }
    public void setDeposito(Integer deposito) { this.deposito = deposito; }

    public EstadoContrato getEstado() { return estado; }
    public void setEstado(EstadoContrato estado) { this.estado = estado; }

    public Integer getIdSolicitud() { return idSolicitud; }
    public void setIdSolicitud(Integer idSolicitud) { this.idSolicitud = idSolicitud; }

    public Integer getDocumentoArrendatario() { return documentoArrendatario; }
    public void setDocumentoArrendatario(Integer documentoArrendatario) { this.documentoArrendatario = documentoArrendatario; }

    public String getNombreArrendatario() { return nombreArrendatario; }
    public void setNombreArrendatario(String nombreArrendatario) { this.nombreArrendatario = nombreArrendatario; }

    public Integer getDocumentoArrendador() { return documentoArrendador; }
    public void setDocumentoArrendador(Integer documentoArrendador) { this.documentoArrendador = documentoArrendador; }

    public String getNombreArrendador() { return nombreArrendador; }
    public void setNombreArrendador(String nombreArrendador) { this.nombreArrendador = nombreArrendador; }

    public MotoResumenDTO getMoto() { return moto; }
    public void setMoto(MotoResumenDTO moto) { this.moto = moto; }
}
