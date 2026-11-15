package com.basesdatos2.backendBD2.repositorio;

import com.basesdatos2.backendBD2.modelo.LogBucket;
import com.basesdatos2.backendBD2.modelo.LogEvento;
import com.basesdatos2.backendBD2.modelo.emun.ModuloLog;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Acceso a la coleccion logs_actividad de MongoDB (patron bucket) con el driver
// oficial de MongoDB, sin ORM: cada consulta se escribe a mano con sus operadores
// ($lt, $gte, $push, $inc...) y los documentos se convierten a mano a LogBucket / LogEvento,
// igual que los repositorios de MySQL escriben su SQL y su metodo mapear().
@Repository
public class LogRepository {
    private static final Logger log = LoggerFactory.getLogger(LogRepository.class);
    private static final String COLECCION = "logs_actividad";

    // Las fechas se guardan como ISODate usando la zona del servidor (America/Bogota),
    // igual que se guardaban antes, para que los documentos existentes sigan sirviendo.
    private static final ZoneId ZONA = ZoneId.systemDefault();

    private final MongoCollection<Document> logs;
    private final int eventosPorBucket;

    public LogRepository(MongoClient mongoClient,
                         @Value("${spring.mongodb.database}") String baseDeDatos,
                         @Value("${app.logs.eventos-por-bucket:50}") int eventosPorBucket) {
        this.logs = mongoClient.getDatabase(baseDeDatos).getCollection(COLECCION);
        this.eventosPorBucket = eventosPorBucket;
    }

    // Indices: el primero sirve para encontrar la cubeta abierta al insertar,
    // el segundo para las consultas del admin por rango de fechas.
    // Corre en segundo plano (@Async) para no bloquear el arranque de Spring Boot.
    //
    //   db.logs_actividad.createIndex({ modulo: 1, dia: 1, cantidad: 1 }, { name: "idx_cubeta_abierta" })
    //   db.logs_actividad.createIndex({ dia: -1 }, { name: "idx_dia" })
    @Async
    public void crearIndices() {
        try {
            logs.createIndex(
                    new Document("modulo", 1).append("dia", 1).append("cantidad", 1),
                    new IndexOptions().name("idx_cubeta_abierta"));
            logs.createIndex(
                    new Document("dia", -1),
                    new IndexOptions().name("idx_dia"));
        } catch (RuntimeException e) {
            log.warn("No se pudieron crear los indices de logs en MongoDB: {}", e.getMessage());
        }
    }

    // Agrega el evento a la cubeta del modulo y dia que todavia tenga espacio.
    // Si no hay ninguna (o todas estan llenas), el upsert crea una nueva:
    // Mongo copia al documento nuevo los campos de igualdad del filtro (modulo, dia)
    // y $inc / $push lo inicializan con cantidad = 1 y el primer evento.
    // Corre en segundo plano (@Async): si Mongo falla solo queda un warning en consola.
    //
    //   db.logs_actividad.updateOne(
    //     { modulo: "PAGO", dia: ISODate("2026-11-14"), cantidad: { $lt: 50 } },
    //     { $push: { eventos: { ...evento } },
    //       $inc:  { cantidad: 1 },
    //       $min:  { primerEvento: ISODate(...) },
    //       $max:  { ultimoEvento: ISODate(...) } },
    //     { upsert: true }
    //   )
    @Async
    public void guardar(ModuloLog modulo, LogEvento evento) {
        LocalDateTime fecha = evento.getFecha() != null ? evento.getFecha() : LocalDateTime.now();

        Document filtro = new Document("modulo", modulo.name())
                .append("dia", aDate(fecha.toLocalDate()))
                .append("cantidad", new Document("$lt", eventosPorBucket));

        Document actualizacion = new Document()
                .append("$push", new Document("eventos", aDocumento(evento)))
                .append("$inc", new Document("cantidad", 1))
                .append("$min", new Document("primerEvento", aDate(fecha)))
                .append("$max", new Document("ultimoEvento", aDate(fecha)));

        try {
            logs.updateOne(filtro, actualizacion, new UpdateOptions().upsert(true));
        } catch (RuntimeException e) {
            log.warn("No se pudo guardar el log {} {} en MongoDB: {}",
                    modulo, evento.getAccion(), e.getMessage());
        }
    }

    // Cubetas cuyo dia esta en el rango (ambos incluidos), opcionalmente de un modulo,
    // de la mas reciente a la mas antigua.
    //
    //   db.logs_actividad.find(
    //     { dia: { $gte: ISODate(desde), $lte: ISODate(hasta) }, modulo: "PAGO" }   // modulo es opcional
    //   ).sort({ dia: -1, ultimoEvento: -1 })
    public List<LogBucket> buscar(LocalDate desde, LocalDate hasta, ModuloLog modulo) {
        Document filtro = new Document("dia", new Document("$gte", aDate(desde)).append("$lte", aDate(hasta)));
        if (modulo != null) {
            filtro.append("modulo", modulo.name());
        }

        Document orden = new Document("dia", -1).append("ultimoEvento", -1);

        List<LogBucket> cubetas = new ArrayList<>();
        for (Document documento : logs.find(filtro).sort(orden)) {
            cubetas.add(mapear(documento));
        }
        return cubetas;
    }

    // ---------------- Conversion Document <-> entidad (hecha a mano) ----------------

    private Document aDocumento(LogEvento evento) {
        return new Document()
                .append("id", evento.getId())
                .append("fecha", aDate(evento.getFecha()))
                .append("accion", evento.getAccion())
                .append("documentoUsuario", evento.getDocumentoUsuario())
                .append("nombreUsuario", evento.getNombreUsuario())
                .append("rolUsuario", evento.getRolUsuario())
                .append("referencia", evento.getReferencia())
                .append("descripcion", evento.getDescripcion())
                .append("detalle", new Document(evento.getDetalle()))
                .append("ip", evento.getIp())
                .append("userAgent", evento.getUserAgent())
                .append("endpoint", evento.getEndpoint());
    }

    private LogBucket mapear(Document documento) {
        LogBucket cubeta = new LogBucket();
        cubeta.setId(texto(documento.get("_id")));
        cubeta.setModulo(ModuloLog.valueOf(documento.getString("modulo")));
        cubeta.setDia(aLocalDate(documento.getDate("dia")));
        cubeta.setCantidad(documento.getInteger("cantidad"));
        cubeta.setPrimerEvento(aLocalDateTime(documento.getDate("primerEvento")));
        cubeta.setUltimoEvento(aLocalDateTime(documento.getDate("ultimoEvento")));

        List<LogEvento> eventos = new ArrayList<>();
        for (Document e : documento.getList("eventos", Document.class, List.of())) {
            eventos.add(mapearEvento(e));
        }
        cubeta.setEventos(eventos);
        return cubeta;
    }

    private LogEvento mapearEvento(Document documento) {
        LogEvento evento = new LogEvento();
        // Los eventos guardados antes con Spring Data pueden traer el id en "_id".
        evento.setId(texto(documento.containsKey("id") ? documento.get("id") : documento.get("_id")));
        evento.setFecha(aLocalDateTime(documento.getDate("fecha")));
        evento.setAccion(documento.getString("accion"));
        evento.setDocumentoUsuario(documento.getInteger("documentoUsuario"));
        evento.setNombreUsuario(documento.getString("nombreUsuario"));
        evento.setRolUsuario(documento.getString("rolUsuario"));
        evento.setReferencia(documento.getString("referencia"));
        evento.setDescripcion(documento.getString("descripcion"));
        Document detalle = documento.get("detalle", Document.class);
        evento.setDetalle(detalle != null ? new LinkedHashMap<String, Object>(detalle) : Map.of());
        evento.setIp(documento.getString("ip"));
        evento.setUserAgent(documento.getString("userAgent"));
        evento.setEndpoint(documento.getString("endpoint"));
        return evento;
    }

    private static String texto(Object valor) {
        return valor != null ? valor.toString() : null;
    }

    private static Date aDate(LocalDate dia) {
        return dia != null ? Date.from(dia.atStartOfDay(ZONA).toInstant()) : null;
    }

    private static Date aDate(LocalDateTime fecha) {
        return fecha != null ? Date.from(fecha.atZone(ZONA).toInstant()) : null;
    }

    private static LocalDate aLocalDate(Date fecha) {
        return fecha != null ? fecha.toInstant().atZone(ZONA).toLocalDate() : null;
    }

    private static LocalDateTime aLocalDateTime(Date fecha) {
        return fecha != null ? LocalDateTime.ofInstant(fecha.toInstant(), ZONA) : null;
    }
}
