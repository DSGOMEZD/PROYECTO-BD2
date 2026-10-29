package com.basesdatos2.backendBD2.repositorio;

import com.basesdatos2.backendBD2.modelo.Contrato;
import com.basesdatos2.backendBD2.modelo.emun.EstadoContrato;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;

import org.springframework.jdbc.core.CallableStatementCallback;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class ContratoRepository {
    private final JdbcTemplate jdbcTemplate;

    public ContratoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String SQL_BASE = """
            SELECT id_contrato, Fecha_inicio, Fecha_fin, Estado_contrato,
                   Numero_documento, Numero_documento1, Id_solicitud, Placa, Deposito
            FROM CONTRATO
            """;

    public List<Contrato> listarMios(Integer documentoArrendatario) {
        return jdbcTemplate.query(
                SQL_BASE + """
                        WHERE Numero_documento = ?
                        ORDER BY Fecha_inicio DESC, id_contrato DESC
                        """,
                this::mapear,
                documentoArrendatario
        );
    }

    public List<Contrato> listarComoArrendador(Integer documentoArrendador) {
        return jdbcTemplate.query(
                SQL_BASE + """
                        WHERE Numero_documento1 = ?
                        ORDER BY Fecha_inicio DESC, id_contrato DESC
                        """,
                this::mapear,
                documentoArrendador
        );
    }

    public Optional<Contrato> buscarPorId(Integer idContrato) {
        return jdbcTemplate.query(SQL_BASE + "WHERE id_contrato = ?", this::mapear, idContrato)
                .stream()
                .findFirst();
    }

    private Contrato mapear(ResultSet rs, int fila) throws SQLException {
        return new Contrato(
                rs.getInt("id_contrato"),
                rs.getDate("Fecha_inicio").toLocalDate(),
                rs.getDate("Fecha_fin").toLocalDate(),
                EstadoContrato.valueOf(rs.getString("Estado_contrato").trim()),
                rs.getInt("Numero_documento"),
                rs.getInt("Numero_documento1"),
                rs.getInt("Id_solicitud"),
                rs.getString("Placa").trim(),
                rs.getInt("Deposito")
        );
    }

    public void cancelar(Integer idContrato, Integer documento) {
        jdbcTemplate.execute(
                "{CALL sp_cerrar_contrato(?, ?, ?, ?)}",
                (CallableStatementCallback<Void>) cs -> {
                    cs.setInt(1, idContrato);
                    cs.setInt(2, documento);
                    cs.setString(3, "CANCELADO");
                    cs.setNull(4, Types.INTEGER);
                    cs.execute();
                    return null;
                }
        );
    }

    public void finalizar(
            Integer idContrato,
            Integer documentoArrendador,
            Integer kilometrajeFinal
    ) {
        jdbcTemplate.execute(
                "{CALL sp_cerrar_contrato(?, ?, ?, ?)}",
                (CallableStatementCallback<Void>) cs -> {
                    cs.setInt(1, idContrato);
                    cs.setInt(2, documentoArrendador);
                    cs.setString(3, "FINALIZADO");
                    cs.setInt(4, kilometrajeFinal);
                    cs.execute();
                    return null;
                }
        );
    }
}
