package com.basesdatos2.backendBD2.modelo;

import com.basesdatos2.backendBD2.modelo.emun.ModuloLog;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

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
@Document(collection = "logs_actividad")
@CompoundIndex(name = "idx_cubeta_abierta", def = "{'modulo': 1, 'dia': 1, 'cantidad': 1}")
public class LogBucket {

    @Id
    private String id;

    @Field("modulo")
    private ModuloLog modulo;

    @Indexed(name = "idx_dia", direction = IndexDirection.DESCENDING)
    @Field("dia")
    private LocalDate dia;

    @Field("cantidad")
    private Integer cantidad;              // eventos guardados en la cubeta

    @Field("primerEvento")
    private LocalDateTime primerEvento;

    @Field("ultimoEvento")
    private LocalDateTime ultimoEvento;

    @Field("eventos")
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

    public List<LogEvento> getEventos() { return eventos != null ? eventos : List.of(); }
    public void setEventos(List<LogEvento> eventos) { this.eventos = eventos != null ? eventos : new ArrayList<>(); }
}
