package com.basesdatos2.backendBD2.Modelo;

import com.basesdatos2.backendBD2.Modelo.emun.EstadoMotocicleta;
import java.math.BigDecimal;

// Tabla MOTOCICLETA
public class Motocicleta {

    private String placa;
    private Integer documentoArrendador;   // columna Numero_documento
    private String marca;
    private String modelo;
    private Integer anio;                  // columna Año
    private BigDecimal cilindraje;
    private Integer kilometrajeActual;
    private Integer precioAlquiler;        // precio por dia
    private EstadoMotocicleta estado;
    private Integer idSeguro;

    public Motocicleta() {}

    public Motocicleta(String placa, Integer documentoArrendador, String marca, String modelo,
                       Integer anio, BigDecimal cilindraje, Integer kilometrajeActual,
                       Integer precioAlquiler, EstadoMotocicleta estado, Integer idSeguro) {
        this.placa = placa;
        this.documentoArrendador = documentoArrendador;
        this.marca = marca;
        this.modelo = modelo;
        this.anio = anio;
        this.cilindraje = cilindraje;
        this.kilometrajeActual = kilometrajeActual;
        this.precioAlquiler = precioAlquiler;
        this.estado = estado;
        this.idSeguro = idSeguro;
    }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public Integer getDocumentoArrendador() { return documentoArrendador; }
    public void setDocumentoArrendador(Integer documentoArrendador) { this.documentoArrendador = documentoArrendador; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }

    public BigDecimal getCilindraje() { return cilindraje; }
    public void setCilindraje(BigDecimal cilindraje) { this.cilindraje = cilindraje; }

    public Integer getKilometrajeActual() { return kilometrajeActual; }
    public void setKilometrajeActual(Integer kilometrajeActual) { this.kilometrajeActual = kilometrajeActual; }

    public Integer getPrecioAlquiler() { return precioAlquiler; }
    public void setPrecioAlquiler(Integer precioAlquiler) { this.precioAlquiler = precioAlquiler; }

    public EstadoMotocicleta getEstado() { return estado; }
    public void setEstado(EstadoMotocicleta estado) { this.estado = estado; }

    public Integer getIdSeguro() { return idSeguro; }
    public void setIdSeguro(Integer idSeguro) { this.idSeguro = idSeguro; }
}
