package com.basesdatos2.backendBD2.repositorio;

import com.basesdatos2.backendBD2.modelo.dto.ContratoResponseDTO;
import com.basesdatos2.backendBD2.modelo.dto.MotoResumenDTO;
import com.basesdatos2.backendBD2.modelo.emun.EstadoContrato;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;

import org.springframework.jdbc.core.CallableStatementCallback;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class ContratoRepository {
    private final JdbcTemplate jdbcTemplate;

    public ContratoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String SQL_BASE = """
            SELECT
                c.id_contrato AS idContrato,
                c.Fecha_inicio AS fechaInicio,
                c.Fecha_fin AS fechaFin,
                fn_calcular_valor_alquiler(
                    m.Placa, m.Numero_documento, c.Fecha_inicio, c.Fecha_fin
                ) AS valorTotal,
                c.Deposito AS deposito,
                c.Estado_contrato AS estado,
                c.Id_solicitud AS idSolicitud,
                c.Numero_documento AS documentoArrendatario,
                fn_nombre_completo(c.Numero_documento) AS nombreArrendatario,
                c.Numero_documento1 AS documentoArrendador,
                fn_nombre_completo(c.Numero_documento1) AS nombreArrendador,
                m.Placa AS placa,
                m.Marca AS marca,
                m.modelo AS modelo,
                m.Precio_alquiler AS precioDia
            FROM CONTRATO c
            JOIN MOTOCICLETA m
              ON m.Placa = c.Placa
             AND m.Numero_documento = c.Numero_documento21
            """;

    public List<ContratoResponseDTO> listarMios(Integer documentoArrendatario) {
        return jdbcTemplate.query(
                SQL_BASE + """
                        WHERE c.Numero_documento = ?
                        ORDER BY c.Fecha_inicio DESC, c.id_contrato DESC
                        """,
                this::mapear,
                documentoArrendatario
        );
    }

    public List<ContratoResponseDTO> listarComoArrendador(Integer documentoArrendador) {
        return jdbcTemplate.query(
                SQL_BASE + """
                        WHERE c.Numero_documento1 = ?
                        ORDER BY c.Fecha_inicio DESC, c.id_contrato DESC
                        """,
                this::mapear,
                documentoArrendador
        );
    }

    private ContratoResponseDTO mapear(ResultSet rs, int fila) throws SQLException {
        return new ContratoResponseDTO(
                rs.getInt("idContrato"),
                rs.getDate("fechaInicio").toLocalDate(),
                rs.getDate("fechaFin").toLocalDate(),
                rs.getInt("valorTotal"),
                rs.getInt("deposito"),
                EstadoContrato.valueOf(rs.getString("estado").trim()),
                rs.getInt("idSolicitud"),
                rs.getInt("documentoArrendatario"),
                rs.getString("nombreArrendatario").trim(),
                rs.getInt("documentoArrendador"),
                rs.getString("nombreArrendador").trim(),
                new MotoResumenDTO(
                        rs.getString("placa").trim(),
                        rs.getString("marca").trim(),
                        rs.getString("modelo").trim(),
                        rs.getInt("precioDia")
                )
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
                (org.springframework.jdbc.core.CallableStatementCallback<Void>) cs -> {
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