package com.basesdatos2.backendBD2.modelo.dto;

import com.basesdatos2.backendBD2.modelo.emun.EstadoMotocicleta;
import java.math.BigDecimal;

// valorEstimado solo viene lleno en el catalogo de motos disponibles por fechas.
public class MotocicletaResponseDTO {

    private String placa;
    private String marca;
    private String modelo;
    private String color;
    private String descripcion;
    private Integer anio;
    private BigDecimal cilindraje;
    private Integer kilometraje;
    private Integer precioDia;
    private EstadoMotocicleta estado;
    private Integer documentoArrendador;
    private String nombreArrendador;
    private SeguroResponseDTO seguro;
    private Integer valorEstimado;

    public MotocicletaResponseDTO() {}

    public MotocicletaResponseDTO(String placa, String marca, String modelo, String color,
                                  String descripcion, Integer anio, BigDecimal cilindraje,
                                  Integer kilometraje, Integer precioDia, EstadoMotocicleta estado,
                                  Integer documentoArrendador, String nombreArrendador,
                                  SeguroResponseDTO seguro, Integer valorEstimado) {
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.color = color;
        this.descripcion = descripcion;
        this.anio = anio;
        this.cilindraje = cilindraje;
        this.kilometraje = kilometraje;
        this.precioDia = precioDia;
        this.estado = estado;
        this.documentoArrendador = documentoArrendador;
        this.nombreArrendador = nombreArrendador;
        this.seguro = seguro;
        this.valorEstimado = valorEstimado;
    }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }

    public BigDecimal getCilindraje() { return cilindraje; }
    public void setCilindraje(BigDecimal cilindraje) { this.cilindraje = cilindraje; }

    public Integer getKilometraje() { return kilometraje; }
    public void setKilometraje(Integer kilometraje) { this.kilometraje = kilometraje; }

    public Integer getPrecioDia() { return precioDia; }
    public void setPrecioDia(Integer precioDia) { this.precioDia = precioDia; }

    public EstadoMotocicleta getEstado() { return estado; }
    public void setEstado(EstadoMotocicleta estado) { this.estado = estado; }

    public Integer getDocumentoArrendador() { return documentoArrendador; }
    public void setDocumentoArrendador(Integer documentoArrendador) { this.documentoArrendador = documentoArrendador; }

    public String getNombreArrendador() { return nombreArrendador; }
    public void setNombreArrendador(String nombreArrendador) { this.nombreArrendador = nombreArrendador; }

    public SeguroResponseDTO getSeguro() { return seguro; }
    public void setSeguro(SeguroResponseDTO seguro) { this.seguro = seguro; }

    public Integer getValorEstimado() { return valorEstimado; }
    public void setValorEstimado(Integer valorEstimado) { this.valorEstimado = valorEstimado; }
}
