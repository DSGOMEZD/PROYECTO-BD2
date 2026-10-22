package com.basesdatos2.backendBD2.Modelo;

import java.time.LocalDate;

// Tabla INFO_MANTENIMIENTO
public class InfoMantenimiento {

    private Integer idMantenimiento;
    private LocalDate fechaMantenimiento;
    private String tipoMantenimiento;
    private Integer kilometrajeAlMomento;
    private String descripcion;
    private Integer costo;
    private String placa;
    private Integer documentoArrendador;  // columna Numero_documento

    public InfoMantenimiento() {}

    public InfoMantenimiento(Integer idMantenimiento, LocalDate fechaMantenimiento,
                             String tipoMantenimiento, Integer kilometrajeAlMomento,
                             String descripcion, Integer costo, String placa,
                             Integer documentoArrendador) {
        this.idMantenimiento = idMantenimiento;
        this.fechaMantenimiento = fechaMantenimiento;
        this.tipoMantenimiento = tipoMantenimiento;
        this.kilometrajeAlMomento = kilometrajeAlMomento;
        this.descripcion = descripcion;
        this.costo = costo;
        this.placa = placa;
        this.documentoArrendador = documentoArrendador;
    }

    public Integer getIdMantenimiento() { return idMantenimiento; }
    public void setIdMantenimiento(Integer idMantenimiento) { this.idMantenimiento = idMantenimiento; }

    public LocalDate getFechaMantenimiento() { return fechaMantenimiento; }
    public void setFechaMantenimiento(LocalDate fechaMantenimiento) { this.fechaMantenimiento = fechaMantenimiento; }

    public String getTipoMantenimiento() { return tipoMantenimiento; }
    public void setTipoMantenimiento(String tipoMantenimiento) { this.tipoMantenimiento = tipoMantenimiento; }

    public Integer getKilometrajeAlMomento() { return kilometrajeAlMomento; }
    public void setKilometrajeAlMomento(Integer kilometrajeAlMomento) { this.kilometrajeAlMomento = kilometrajeAlMomento; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getCosto() { return costo; }
    public void setCosto(Integer costo) { this.costo = costo; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public Integer getDocumentoArrendador() { return documentoArrendador; }
    public void setDocumentoArrendador(Integer documentoArrendador) { this.documentoArrendador = documentoArrendador; }
}
