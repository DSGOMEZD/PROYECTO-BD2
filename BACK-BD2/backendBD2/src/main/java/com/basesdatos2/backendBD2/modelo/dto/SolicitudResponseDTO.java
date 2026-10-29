package com.basesdatos2.backendBD2.modelo.dto;

import com.basesdatos2.backendBD2.modelo.emun.EstadoSolicitud;
import java.time.LocalDate;

// fechaInicio y fechaFin vienen del contrato asociado a la solicitud.
public class SolicitudResponseDTO {

    private Integer idSolicitud;
    private LocalDate fechaSolicitud;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String mensaje;
    private EstadoSolicitud estado;
    private Integer documentoArrendatario;
    private String nombreArrendatario;
    private Integer documentoArrendador;
    private MotoResumenDTO moto;

    public SolicitudResponseDTO() {}

    public SolicitudResponseDTO(Integer idSolicitud, LocalDate fechaSolicitud, LocalDate fechaInicio,
                                LocalDate fechaFin, String mensaje, EstadoSolicitud estado,
                                Integer documentoArrendatario, String nombreArrendatario,
                                Integer documentoArrendador, MotoResumenDTO moto) {
        this.idSolicitud = idSolicitud;
        this.fechaSolicitud = fechaSolicitud;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.mensaje = mensaje;
        this.estado = estado;
        this.documentoArrendatario = documentoArrendatario;
        this.nombreArrendatario = nombreArrendatario;
        this.documentoArrendador = documentoArrendador;
        this.moto = moto;
    }

    public Integer getIdSolicitud() { return idSolicitud; }
    public void setIdSolicitud(Integer idSolicitud) { this.idSolicitud = idSolicitud; }

    public LocalDate getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDate fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public EstadoSolicitud getEstado() { return estado; }
    public void setEstado(EstadoSolicitud estado) { this.estado = estado; }

    public Integer getDocumentoArrendatario() { return documentoArrendatario; }
    public void setDocumentoArrendatario(Integer documentoArrendatario) { this.documentoArrendatario = documentoArrendatario; }

    public String getNombreArrendatario() { return nombreArrendatario; }
    public void setNombreArrendatario(String nombreArrendatario) { this.nombreArrendatario = nombreArrendatario; }

    public Integer getDocumentoArrendador() { return documentoArrendador; }
    public void setDocumentoArrendador(Integer documentoArrendador) { this.documentoArrendador = documentoArrendador; }

    public MotoResumenDTO getMoto() { return moto; }
    public void setMoto(MotoResumenDTO moto) { this.moto = moto; }
}
