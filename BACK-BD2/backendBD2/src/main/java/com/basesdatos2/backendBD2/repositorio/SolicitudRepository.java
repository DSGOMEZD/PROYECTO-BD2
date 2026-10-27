package com.basesdatos2.backendBD2.repositorio;

import com.basesdatos2.backendBD2.modelo.dto.ContratoResponseDTO;
import com.basesdatos2.backendBD2.modelo.dto.MotoResumenDTO;
import com.basesdatos2.backendBD2.modelo.dto.SolicitudRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.SolicitudResponseDTO;
import com.basesdatos2.backendBD2.modelo.emun.EstadoContrato;
import com.basesdatos2.backendBD2.modelo.emun.EstadoSolicitud;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;
import java.util.Map;

@Repository
public class SolicitudRepository {
    private final JdbcTemplate jdbcTemplate;

    public SolicitudRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SolicitudResponseDTO crear(
            Integer documentoArrendatario,
            SolicitudRequestDTO dto
    ) {
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
                        .addValue("p_arrendatario", documentoArrendatario)
                        .addValue("p_placa", dto.placa())
                        .addValue("p_fecha_inicio", dto.fechaInicio())
                        .addValue("p_fecha_fin", dto.fechaFin())
                        // SOLICITUD.Mensaje_solicitud es NOT NULL en la BD.
                        .addValue("p_mensaje",
                                dto.mensaje() == null ? "" : dto.mensaje())
        );

        Integer idSolicitud =
                ((Number) salida.get("p_id_solicitud")).intValue();

        return buscarPorId(idSolicitud);
    }

    public List<SolicitudResponseDTO> listarMias(Integer documentoArrendatario) {
        return jdbcTemplate.query(
                sqlBase() + """
                WHERE s.Numero_documento = ?
                ORDER BY s.Fecha_solicitud DESC, s.Id_solicitud DESC
                """,
                this::mapearSolicitud,
                documentoArrendatario
        );
    }

    public List<SolicitudResponseDTO> listarRecibidas(Integer documentoArrendador) {
        return jdbcTemplate.query(
                sqlBase() + """
                WHERE s.Numero_documento2 = ?
                ORDER BY s.Fecha_solicitud DESC, s.Id_solicitud DESC
                """,
                this::mapearSolicitud,
                documentoArrendador
        );
    }

    private SolicitudResponseDTO buscarPorId(Integer idSolicitud) {
        List<SolicitudResponseDTO> resultados = jdbcTemplate.query(
                sqlBase() + "WHERE s.Id_solicitud = ?",
                this::mapearSolicitud,
                idSolicitud
        );

        return resultados.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "La solicitud fue creada, pero no se pudo consultar."
                ));
    }

    private String sqlBase() {
        return """
                SELECT
                    s.Id_solicitud AS idSolicitud,
                    s.Fecha_solicitud AS fechaSolicitud,
                    c.Fecha_inicio AS fechaInicio,
                    c.Fecha_fin AS fechaFin,
                    s.Mensaje_solicitud AS mensaje,
                    s.Estado_solicitud AS estado,
                    s.Numero_documento AS documentoArrendatario,
                    fn_nombre_completo(s.Numero_documento) AS nombreArrendatario,
                    s.Numero_documento2 AS documentoArrendador,
                    m.Placa AS placa,
                    m.Marca AS marca,
                    m.modelo AS modelo,
                    m.Precio_alquiler AS precioDia
                FROM SOLICITUD s
                JOIN CONTRATO c
                  ON c.id_contrato = s.id_contrato
                 AND c.Numero_documento = s.Numero_documento1
                 AND c.Numero_documento1 = s.Numero_documento11
                JOIN MOTOCICLETA m
                  ON m.Placa = s.Placa
                 AND m.Numero_documento = s.Numero_documento2
                """;
    }

    private SolicitudResponseDTO mapearSolicitud(
            java.sql.ResultSet rs,
            int fila
    ) throws java.sql.SQLException {
        return new SolicitudResponseDTO(
                rs.getInt("idSolicitud"),
                rs.getDate("fechaSolicitud").toLocalDate(),
                rs.getDate("fechaInicio").toLocalDate(),
                rs.getDate("fechaFin").toLocalDate(),
                rs.getString("mensaje").trim(),
                EstadoSolicitud.valueOf(rs.getString("estado").trim()),
                rs.getInt("documentoArrendatario"),
                rs.getString("nombreArrendatario").trim(),
                rs.getInt("documentoArrendador"),
                new MotoResumenDTO(
                        rs.getString("placa").trim(),
                        rs.getString("marca").trim(),
                        rs.getString("modelo").trim(),
                        rs.getInt("precioDia")
                )
        );
    }

    public ContratoResponseDTO aprobar(Integer idSolicitud, Integer documentoArrendador) {
        responder(idSolicitud, documentoArrendador, "APROBADA");

        String sql = """
        SELECT
            c.id_contrato AS idContrato,
            c.Fecha_inicio AS fechaInicio,
            c.Fecha_fin AS fechaFin,
            fn_calcular_valor_alquiler(
                m.Placa, m.Numero_documento, c.Fecha_inicio, c.Fecha_fin
            ) AS valorTotal,
            c.Estado_contrato AS estado,
            s.Id_solicitud AS idSolicitud,
            c.Numero_documento AS documentoArrendatario,
            fn_nombre_completo(c.Numero_documento) AS nombreArrendatario,
            c.Numero_documento1 AS documentoArrendador,
            fn_nombre_completo(c.Numero_documento1) AS nombreArrendador,
            m.Placa AS placa,
            m.Marca AS marca,
            m.modelo AS modelo,
            m.Precio_alquiler AS precioDia
        FROM CONTRATO c
        JOIN SOLICITUD s
          ON s.id_contrato = c.id_contrato
         AND s.Numero_documento1 = c.Numero_documento
         AND s.Numero_documento11 = c.Numero_documento1
        JOIN MOTOCICLETA m
          ON m.Placa = s.Placa
         AND m.Numero_documento = s.Numero_documento2
        WHERE s.Id_solicitud = ?
        """;

        return jdbcTemplate.queryForObject(sql, (rs, fila) ->
                        new ContratoResponseDTO(
                                rs.getInt("idContrato"),
                                rs.getDate("fechaInicio").toLocalDate(),
                                rs.getDate("fechaFin").toLocalDate(),
                                rs.getInt("valorTotal"),
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
                        ),
                idSolicitud
        );
    }

    public void rechazar(Integer idSolicitud, Integer documentoArrendador) {
        responder(idSolicitud, documentoArrendador, "RECHAZADA");
    }

    private void responder(
            Integer idSolicitud,
            Integer documentoArrendador,
            String decision
    ) {
        SimpleJdbcCall llamada = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_responder_solicitud")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_id_solicitud", Types.INTEGER),
                        new SqlParameter("p_arrendador", Types.INTEGER),
                        new SqlParameter("p_decision", Types.CHAR),
                        new SqlOutParameter("p_id_pago", Types.INTEGER)
                );

        llamada.execute(new MapSqlParameterSource()
                .addValue("p_id_solicitud", idSolicitud)
                .addValue("p_arrendador", documentoArrendador)
                .addValue("p_decision", decision));
    }
}
