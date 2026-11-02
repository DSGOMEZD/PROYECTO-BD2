package com.basesdatos2.backendBD2.repositorio;

import com.basesdatos2.backendBD2.modelo.LogBucket;
import com.basesdatos2.backendBD2.modelo.LogEvento;
import com.basesdatos2.backendBD2.modelo.emun.ModuloLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

// Acceso a la coleccion logs_actividad de MongoDB (patron bucket).
@Repository
public class LogRepository {
    private static final Logger log = LoggerFactory.getLogger(LogRepository.class);

    private final MongoTemplate mongoTemplate;
    private final int eventosPorBucket;

    public LogRepository(MongoTemplate mongoTemplate,
                         @Value("${app.logs.eventos-por-bucket:50}") int eventosPorBucket) {
        this.mongoTemplate = mongoTemplate;
        this.eventosPorBucket = eventosPorBucket;
    }

    // Indices: el primero sirve para encontrar la cubeta abierta al insertar,
    // el segundo para las consultas del admin por rango de fechas.
    public void crearIndices() {
        var indices = mongoTemplate.indexOps(LogBucket.class);
        indices.createIndex(new Index()
                .on("modulo", Sort.Direction.ASC)
                .on("dia", Sort.Direction.ASC)
                .on("cantidad", Sort.Direction.ASC)
                .named("idx_cubeta_abierta"));
        indices.createIndex(new Index()
                .on("dia", Sort.Direction.DESC)
                .named("idx_dia"));
    }

    // Agrega el evento a la cubeta del modulo y dia que todavia tenga espacio.
    // Si no hay ninguna (o todas estan llenas), el upsert crea una nueva:
    // Mongo copia al documento nuevo los campos de igualdad del filtro (modulo, dia)
    // y $inc / $push lo inicializan con cantidad = 1 y el primer evento.
    // Corre en segundo plano (@Async): si Mongo falla solo queda un warning en consola.
    @Async
    public void guardar(ModuloLog modulo, LogEvento evento) {
        Query cubetaAbierta = new Query(Criteria.where("modulo").is(modulo)
                .and("dia").is(evento.getFecha().toLocalDate())
                .and("cantidad").lt(eventosPorBucket));

        Update agregar = new Update()
                .push("eventos", evento)
                .inc("cantidad", 1)
                .min("primerEvento", evento.getFecha())
                .max("ultimoEvento", evento.getFecha());

        try {
            mongoTemplate.upsert(cubetaAbierta, agregar, LogBucket.class);
        } catch (RuntimeException e) {
            log.warn("No se pudo guardar el log {} {} en MongoDB: {}",
                    modulo, evento.getAccion(), e.getMessage());
        }
    }

    // Cubetas cuyo dia esta en el rango (ambos incluidos), opcionalmente de un modulo.
    public List<LogBucket> buscar(LocalDate desde, LocalDate hasta, ModuloLog modulo) {
        Criteria criterio = Criteria.where("dia").gte(desde).lte(hasta);
        if (modulo != null) {
            criterio = criterio.and("modulo").is(modulo);
        }
        return mongoTemplate.find(new Query(criterio), LogBucket.class);
    }
}
