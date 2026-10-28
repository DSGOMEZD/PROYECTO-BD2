package com.basesdatos2.backendBD2.repositorio;

import com.basesdatos2.backendBD2.modelo.dto.PagoResponseDTO;
import com.basesdatos2.backendBD2.modelo.emun.EstadoPago;
import com.basesdatos2.backendBD2.modelo.emun.MetodoPago;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

@Repository
public class PagoRepository {
    private final JdbcTemplate jdbcTemplate;

    public PagoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String SQL_BASE = """
        SELECT
            p.Id_pago AS idPago,
            p.Monto AS monto,
            p.Fecha_vencimiento AS fechaVencimiento,
            p.fecha_pago AS fechaPago,
            p.Estado AS estado,
            p.Metodo_pago AS metodoPago,
            p.id_contrato AS idContrato
        FROM PAGO p
        JOIN CONTRATO c
          ON c.id_contrato = p.id_contrato
         AND c.Numero_documento = p.Numero_documento
         AND c.Numero_documento1 = p.Numero_documento1
        """;

    public List<PagoResponseDTO> listarMios(Integer documentoArrendatario) {
        return jdbcTemplate.query(
                SQL_BASE + """
                    WHERE c.Numero_documento = ?
                    ORDER BY p.Fecha_vencimiento, p.Id_pago
                    """,
                this::mapear,
                documentoArrendatario
        );
    }

    public List<PagoResponseDTO> listarRecibidos(Integer documentoArrendador) {
        return jdbcTemplate.query(
                SQL_BASE + """
                    WHERE c.Numero_documento1 = ?
                    ORDER BY p.Fecha_vencimiento, p.Id_pago
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

    private PagoResponseDTO mapear(ResultSet rs, int fila) throws SQLException {
        java.sql.Date fechaPago = rs.getDate("fechaPago");
        String metodoPago = rs.getString("metodoPago");

        return new PagoResponseDTO(
                rs.getInt("idPago"),
                rs.getInt("monto"),
                rs.getDate("fechaVencimiento").toLocalDate(),
                fechaPago == null ? null : fechaPago.toLocalDate(),
                EstadoPago.valueOf(rs.getString("estado").trim()),
                metodoPago == null ? null : MetodoPago.valueOf(metodoPago.trim()),
                rs.getInt("idContrato")
        );
    }
}