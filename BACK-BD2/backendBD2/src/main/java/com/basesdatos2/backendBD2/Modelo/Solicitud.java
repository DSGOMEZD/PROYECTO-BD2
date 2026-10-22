package com.basesdatos2.backendBD2.Modelo;

import com.basesdatos2.backendBD2.Modelo.emun.EstadoSolicitud;
import java.time.LocalDate;

// Tabla SOLICITUD
// Las columnas repetidas de la tabla (Numero_documento1, Numero_documento11)
// guardan los mismos documentos que documentoArrendatario y documentoArrendador.
public class Solicitud {

    private Integer idSolicitud;
    private LocalDate fechaSolicitud;
    private EstadoSolicitud estado;
    private String mensaje;
    private Integer documentoArrendatario;  // columna Numero_documento
    private String placa;
    private Integer documentoArrendador;    // columna Numero_documento2
    private Integer idContrato;

    public Solicitud() {}

    public Solicitud(Integer idSolicitud, LocalDate fechaSolicitud, EstadoSolicitud estado,
                     String mensaje, Integer documentoArrendatario, String placa,
                     Integer documentoArrendador, Integer idContrato) {
        this.idSolicitud = idSolicitud;
        this.fechaSolicitud = fechaSolicitud;
        this.estado = estado;
        this.mensaje = mensaje;
        this.documentoArrendatario = documentoArrendatario;
        this.placa = placa;
        this.documentoArrendador = documentoArrendador;
        this.idContrato = idContrato;
    }

    public Integer getIdSolicitud() { return idSolicitud; }
    public void setIdSolicitud(Integer idSolicitud) { this.idSolicitud = idSolicitud; }

    public LocalDate getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDate fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    public EstadoSolicitud getEstado() { return estado; }
    public void setEstado(EstadoSolicitud estado) { this.estado = estado; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public Integer getDocumentoArrendatario() { return documentoArrendatario; }
    public void setDocumentoArrendatario(Integer documentoArrendatario) { this.documentoArrendatario = documentoArrendatario; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public Integer getDocumentoArrendador() { return documentoArrendador; }
    public void setDocumentoArrendador(Integer documentoArrendador) { this.documentoArrendador = documentoArrendador; }

    public Integer getIdContrato() { return idContrato; }
    public void setIdContrato(Integer idContrato) { this.idContrato = idContrato; }
}
