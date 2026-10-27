package com.basesdatos2.backendBD2.modelo;

// Tabla ARRENDATARIO (subtipo de USUARIO): quien alquila motocicletas
public class Arrendatario {

    private Integer numeroDocumento;

    public Arrendatario() {}

    public Arrendatario(Integer numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public Integer getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(Integer numeroDocumento) { this.numeroDocumento = numeroDocumento; }
}
