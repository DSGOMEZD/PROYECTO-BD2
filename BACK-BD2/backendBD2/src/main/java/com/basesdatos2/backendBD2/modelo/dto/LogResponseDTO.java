package com.basesdatos2.backendBD2.modelo.dto;

import com.basesdatos2.backendBD2.modelo.emun.ModuloLog;

import java.time.LocalDateTime;
import java.util.Map;

// Evento que devuelve GET /api/admin/logs (ya sacado de su cubeta).
public class LogResponseDTO {

    private String id;
    private LocalDateTime fecha;
    private ModuloLog modulo;
    private String accion;
    private Integer documentoUsuario;
    private String nombreUsuario;
    private String rolUsuario;
    private String referencia;
    private String descripcion;
    private Map<String, Object> detalle;
    private String ip;
    private String userAgent;
    private String endpoint;

    public LogResponseDTO() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public ModuloLog getModulo() { return modulo; }
    public void setModulo(ModuloLog modulo) { this.modulo = modulo; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public Integer getDocumentoUsuario() { return documentoUsuario; }
    public void setDocumentoUsuario(Integer documentoUsuario) { this.documentoUsuario = documentoUsuario; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getRolUsuario() { return rolUsuario; }
    public void setRolUsuario(String rolUsuario) { this.rolUsuario = rolUsuario; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Map<String, Object> getDetalle() { return detalle; }
    public void setDetalle(Map<String, Object> detalle) { this.detalle = detalle; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
}
