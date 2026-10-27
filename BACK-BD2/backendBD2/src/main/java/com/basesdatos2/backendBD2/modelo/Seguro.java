package com.basesdatos2.backendBD2.modelo;

import com.basesdatos2.backendBD2.modelo.emun.EstadoPoliza;
import java.time.LocalDate;

// Tabla SEGURO
public class Seguro {

    private Integer idSeguro;
    private Integer numeroPoliza;
    private String aseguradora;
    private LocalDate fechaInicioVigencia;
    private EstadoPoliza estadoPoliza;

    public Seguro() {}

    public Seguro(Integer idSeguro, Integer numeroPoliza, String aseguradora,
                  LocalDate fechaInicioVigencia, EstadoPoliza estadoPoliza) {
        this.idSeguro = idSeguro;
        this.numeroPoliza = numeroPoliza;
        this.aseguradora = aseguradora;
        this.fechaInicioVigencia = fechaInicioVigencia;
        this.estadoPoliza = estadoPoliza;
    }

    public Integer getIdSeguro() { return idSeguro; }
    public void setIdSeguro(Integer idSeguro) { this.idSeguro = idSeguro; }

    public Integer getNumeroPoliza() { return numeroPoliza; }
    public void setNumeroPoliza(Integer numeroPoliza) { this.numeroPoliza = numeroPoliza; }

    public String getAseguradora() { return aseguradora; }
    public void setAseguradora(String aseguradora) { this.aseguradora = aseguradora; }

    public LocalDate getFechaInicioVigencia() { return fechaInicioVigencia; }
    public void setFechaInicioVigencia(LocalDate fechaInicioVigencia) { this.fechaInicioVigencia = fechaInicioVigencia; }

    public EstadoPoliza getEstadoPoliza() { return estadoPoliza; }
    public void setEstadoPoliza(EstadoPoliza estadoPoliza) { this.estadoPoliza = estadoPoliza; }
}
