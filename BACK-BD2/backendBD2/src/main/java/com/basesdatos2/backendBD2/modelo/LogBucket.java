package com.basesdatos2.backendBD2.modelo;

import com.basesdatos2.backendBD2.modelo.emun.ModuloLog;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Coleccion logs_actividad (MongoDB) con el patron bucket (cubeta):
// en vez de un documento por evento, cada documento agrupa los eventos de un
// modulo en un dia, hasta un maximo (app.logs.eventos-por-bucket). Cuando la
// cubeta se llena, el siguiente evento abre otra cubeta para el mismo modulo y dia.
//
// {
//   _id, modulo: "PAGO", dia: ISODate("2026-11-01"),
//   cantidad: 3, primerEvento: ISODate(...), ultimoEvento: ISODate(...),
//   eventos: [ { id, fecha, accion, documentoUsuario, ..., detalle: {...} }, ... ]
// }
@Document("logs_actividad")
public class LogBucket {

    @Id
    private String id;
    private ModuloLog modulo;
    private LocalDate dia;
    private Integer cantidad;              // eventos guardados en la cubeta
    private LocalDateTime primerEvento;
    private LocalDateTime ultimoEvento;
    private List<LogEvento> eventos = new ArrayList<>();

    public LogBucket() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public ModuloLog getModulo() { return modulo; }
    public void setModulo(ModuloLog modulo) { this.modulo = modulo; }

    public LocalDate getDia() { return dia; }
    public void setDia(LocalDate dia) { this.dia = dia; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public LocalDateTime getPrimerEvento() { return primerEvento; }
    public void setPrimerEvento(LocalDateTime primerEvento) { this.primerEvento = primerEvento; }

    public LocalDateTime getUltimoEvento() { return ultimoEvento; }
    public void setUltimoEvento(LocalDateTime ultimoEvento) { this.ultimoEvento = ultimoEvento; }

    public List<LogEvento> getEventos() { return eventos; }
    public void setEventos(List<LogEvento> eventos) { this.eventos = eventos; }
}
