package com.basesdatos2.backendBD2.modelo.dto;

// Datos basicos de la moto que se muestran dentro de solicitudes y contratos.
public class MotoResumenDTO {

    private String placa;
    private String marca;
    private String modelo;
    private Integer precioDia;

    public MotoResumenDTO() {}

    public MotoResumenDTO(String placa, String marca, String modelo, Integer precioDia) {
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.precioDia = precioDia;
    }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public Integer getPrecioDia() { return precioDia; }
    public void setPrecioDia(Integer precioDia) { this.precioDia = precioDia; }
}
