package com.basesdatos2.backendBD2.repositorio;

import com.basesdatos2.backendBD2.modelo.Contrato;
import com.basesdatos2.backendBD2.modelo.Solicitud;
import com.basesdatos2.backendBD2.modelo.emun.EstadoSolicitud;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class SolicitudRepository {
    private final JdbcTemplate jdbcTemplate;

    public SolicitudRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String SQL_BASE = """
            SELECT Id_solicitud, Fecha_solicitud, Estado_solicitud, Mensaje_solicitud,
                   Numero_documento, Placa, Numero_documento2, id_contrato
            FROM SOLICITUD
            """;

    // sp_crear_solicitud crea la solicitud y su contrato PENDIENTE (con las fechas).
    // Devuelve el id de la solicitud creada.
    public Integer crear(Solicitud solicitud, Contrato contrato) {
        SimpleJdbcCall llamada = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_crear_solicitud")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_arrendatario", Types.INTEGER),
                        new SqlParameter("p_placa", Types.CHAR),
                        new SqlParameter("p_fecha_inicio", Types.DATE),
                        new SqlParameter("p_fecha_fin", Types.DATE),
                        new SqlParameter("p_mensaje", Types.CHAR),
                        new SqlOutParameter("p_id_solicitud", Types.INTEGER),
                        new SqlOutParameter("p_id_contrato", Types.INTEGER)
                );

        Map<String, Object> salida = llamada.execute(
                new MapSqlParameterSource()
                        .addValue("p_arrendatario", solicitud.getDocumentoArrendatario())
                        .addValue("p_placa", solicitud.getPlaca())
                        .addValue("p_fecha_inicio", contrato.getFechaInicio())
                        .addValue("p_fecha_fin", contrato.getFechaFin())
                        // SOLICITUD.Mensaje_solicitud es NOT NULL en la BD.
                        .addValue("p_mensaje",
                                solicitud.getMensaje() == null ? "" : solicitud.getMensaje())
        );

        return ((Number) salida.get("p_id_solicitud")).intValue();
    }

    public List<Solicitud> listarMias(Integer documentoArrendatario) {
        return jdbcTemplate.query(
                SQL_BASE + """
                        WHERE Numero_documento = ?
                        ORDER BY Fecha_solicitud DESC, Id_solicitud DESC
                        """,
                this::mapear,
                documentoArrendatario
        );
    }

    public List<Solicitud> listarRecibidas(Integer documentoArrendador) {
        return jdbcTemplate.query(
                SQL_BASE + """
                        WHERE Numero_documento2 = ?
                        ORDER BY Fecha_solicitud DESC, Id_solicitud DESC
                        """,
                this::mapear,
                documentoArrendador
        );
    }

    public Optional<Solicitud> buscarPorId(Integer idSolicitud) {
        return jdbcTemplate.query(SQL_BASE + "WHERE Id_solicitud = ?", this::mapear, idSolicitud)
                .stream()
                .findFirst();
    }

    private Solicitud mapear(ResultSet rs, int fila) throws SQLException {
        return new Solicitud(
                rs.getInt("Id_solicitud"),
                rs.getDate("Fecha_solicitud").toLocalDate(),
                EstadoSolicitud.valueOf(rs.getString("Estado_solicitud").trim()),
                rs.getString("Mensaje_solicitud").trim(),
                rs.getInt("Numero_documento"),
                rs.getString("Placa").trim(),
                rs.getInt("Numero_documento2"),
                rs.getInt("id_contrato")
        );
    }

    public void aprobar(Integer idSolicitud, Integer documentoArrendador, Integer deposito) {
        responder(idSolicitud, documentoArrendador, "APROBADA", deposito);
    }

    // Al rechazar no hay deposito, pero el procedimiento lo exige.
    public void rechazar(Integer idSolicitud, Integer documentoArrendador) {
        responder(idSolicitud, documentoArrendador, "RECHAZADA", 0);
    }

    private void responder(
            Integer idSolicitud,
            Integer documentoArrendador,
            String decision,
            Integer deposito
    ) {
        SimpleJdbcCall llamada = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_responder_solicitud")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_id_solicitud", Types.INTEGER),
                        new SqlParameter("p_arrendador", Types.INTEGER),
                        new SqlParameter("p_decision", Types.CHAR),
                        new SqlParameter("p_deposito", Types.INTEGER),
                        new SqlOutParameter("p_id_pago", Types.INTEGER)
                );

        llamada.execute(new MapSqlParameterSource()
                .addValue("p_id_solicitud", idSolicitud)
                .addValue("p_arrendador", documentoArrendador)
                .addValue("p_decision", decision)
                .addValue("p_deposito", deposito));
    }

    public void cancelar(Integer idSolicitud, Integer documentoArrendatario) {
        SimpleJdbcCall llamada = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_cancelar_solicitud")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_id_solicitud", Types.INTEGER),
                        new SqlParameter("p_arrendatario", Types.INTEGER)
                );

        llamada.execute(new MapSqlParameterSource()
                .addValue("p_id_solicitud", idSolicitud)
                .addValue("p_arrendatario", documentoArrendatario));
    }
}
