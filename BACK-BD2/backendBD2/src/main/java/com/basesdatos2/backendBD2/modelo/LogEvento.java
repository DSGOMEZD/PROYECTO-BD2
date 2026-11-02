package com.basesdatos2.backendBD2.modelo;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// Un evento de actividad. No es una coleccion propia: vive dentro del arreglo
// "eventos" de un LogBucket (patron bucket).
public class LogEvento {

    private String id;                    // ObjectId en hexadecimal, generado por el back
    private LocalDateTime fecha;
    private String accion;                // PUBLICADA, CREADA, APROBADA, GENERADO, PAGADO...
    private Integer documentoUsuario;     // quien hizo la accion
    private String nombreUsuario;
    private String rolUsuario;
    private String referencia;            // placa, "Solicitud #5", "Contrato #3", "Pago #2"
    private String descripcion;           // texto legible para el admin
    private Map<String, Object> detalle = new LinkedHashMap<>();  // snapshot de los datos

    // Datos de la peticion HTTP que origino el evento
    private String ip;
    private String userAgent;
    private String endpoint;              // "PATCH /api/pagos/2/pagar"

    public LogEvento() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

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
