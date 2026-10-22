package com.basesdatos2.backendBD2.Modelo;

// Tabla ADMINISTRADOR (subtipo de USUARIO)
public class Administrador {

    private Integer numeroDocumento;

    public Administrador() {}

    public Administrador(Integer numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public Integer getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(Integer numeroDocumento) { this.numeroDocumento = numeroDocumento; }
}
