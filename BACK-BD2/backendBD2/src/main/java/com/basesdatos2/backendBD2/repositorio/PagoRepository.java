package com.basesdatos2.backendBD2.repositorio;

import com.basesdatos2.backendBD2.modelo.Pago;
import com.basesdatos2.backendBD2.modelo.emun.EstadoPago;
import com.basesdatos2.backendBD2.modelo.emun.MetodoPago;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class PagoRepository {
    private final JdbcTemplate jdbcTemplate;

    public PagoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String SQL_BASE = """
        SELECT Id_pago, fecha_pago, Monto, Fecha_vencimiento, Estado, Metodo_pago,
               id_contrato, Numero_documento, Numero_documento1
        FROM PAGO
        """;

    public List<Pago> listarMios(Integer documentoArrendatario) {
        return jdbcTemplate.query(
                SQL_BASE + """
                    WHERE Numero_documento = ?
                    ORDER BY Fecha_vencimiento, Id_pago
                    """,
                this::mapear,
                documentoArrendatario
        );
    }

    public List<Pago> listarRecibidos(Integer documentoArrendador) {
        return jdbcTemplate.query(
                SQL_BASE + """
                    WHERE Numero_documento1 = ?
                    ORDER BY Fecha_vencimiento, Id_pago
                    """,
                this::mapear,
                documentoArrendador
        );
    }

    public void registrar(Integer idPago, Integer documentoArrendatario, MetodoPago metodoPago) {
        jdbcTemplate.execute(
                "{CALL sp_registrar_pago(?, ?, ?)}",
                (CallableStatementCallback<Void>) cs -> {
                    cs.setInt(1, idPago);
                    cs.setInt(2, documentoArrendatario);
                    cs.setString(3, metodoPago.name());
                    cs.execute();
                    return null;
                }
        );
    }

    private Pago mapear(ResultSet rs, int fila) throws SQLException {
        return new Pago(
                rs.getInt("Id_pago"),
                rs.getDate("fecha_pago").toLocalDate(),
                rs.getInt("Monto"),
                rs.getDate("Fecha_vencimiento").toLocalDate(),
                EstadoPago.valueOf(rs.getString("Estado").trim()),
                MetodoPago.valueOf(rs.getString("Metodo_pago").trim()),
                rs.getInt("id_contrato"),
                rs.getInt("Numero_documento"),
                rs.getInt("Numero_documento1")
        );
    }
}
