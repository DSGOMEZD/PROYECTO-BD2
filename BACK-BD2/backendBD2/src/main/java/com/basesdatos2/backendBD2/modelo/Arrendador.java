package com.basesdatos2.backendBD2.modelo;

// Tabla ARRENDADOR (subtipo de USUARIO): dueño de motocicletas
public class Arrendador {

    private Integer numeroDocumento;

    public Arrendador() {}

    public Arrendador(Integer numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public Integer getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(Integer numeroDocumento) { this.numeroDocumento = numeroDocumento; }
}
