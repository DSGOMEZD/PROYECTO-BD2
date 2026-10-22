package com.basesdatos2.backendBD2.Modelo;

import com.basesdatos2.backendBD2.Modelo.emun.EstadoContrato;
import java.time.LocalDate;

// Tabla CONTRATO
// Las columnas repetidas de la tabla (Numero_documento2, Numero_documento21)
// guardan los mismos documentos que documentoArrendatario y documentoArrendador.
public class Contrato {

    private Integer idContrato;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private EstadoContrato estado;
    private Integer documentoArrendatario;  // columna Numero_documento
    private Integer documentoArrendador;    // columna Numero_documento1
    private Integer idSolicitud;
    private String placa;

    public Contrato() {}

    public Contrato(Integer idContrato, LocalDate fechaInicio, LocalDate fechaFin,
                    EstadoContrato estado, Integer documentoArrendatario,
                    Integer documentoArrendador, Integer idSolicitud, String placa) {
        this.idContrato = idContrato;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
        this.documentoArrendatario = documentoArrendatario;
        this.documentoArrendador = documentoArrendador;
        this.idSolicitud = idSolicitud;
        this.placa = placa;
    }

    public Integer getIdContrato() { return idContrato; }
    public void setIdContrato(Integer idContrato) { this.idContrato = idContrato; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public EstadoContrato getEstado() { return estado; }
    public void setEstado(EstadoContrato estado) { this.estado = estado; }

    public Integer getDocumentoArrendatario() { return documentoArrendatario; }
    public void setDocumentoArrendatario(Integer documentoArrendatario) { this.documentoArrendatario = documentoArrendatario; }

    public Integer getDocumentoArrendador() { return documentoArrendador; }
    public void setDocumentoArrendador(Integer documentoArrendador) { this.documentoArrendador = documentoArrendador; }

    public Integer getIdSolicitud() { return idSolicitud; }
    public void setIdSolicitud(Integer idSolicitud) { this.idSolicitud = idSolicitud; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
}
