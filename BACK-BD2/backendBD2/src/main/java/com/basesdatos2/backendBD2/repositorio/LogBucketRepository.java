package com.basesdatos2.backendBD2.repositorio;

import com.basesdatos2.backendBD2.modelo.LogBucket;
import com.basesdatos2.backendBD2.modelo.emun.ModuloLog;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

// Repositorio Spring Data MongoDB mapeado a la coleccion "logs_actividad" (base alquiler_motos_logs en MV2).
@Repository
public interface LogBucketRepository extends MongoRepository<LogBucket, String> {

    @Query("{ 'dia': { $gte: ?0, $lte: ?1 } }")
    List<LogBucket> buscarPorRangoFechas(LocalDate desde, LocalDate hasta, Sort sort);

    @Query("{ 'dia': { $gte: ?0, $lte: ?1 }, 'modulo': ?2 }")
    List<LogBucket> buscarPorRangoFechasYModulo(LocalDate desde, LocalDate hasta, ModuloLog modulo, Sort sort);
}

