package com.basesdatos2.backendBD2.repositorio;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.basesdatos2.backendBD2.modelo.Motocicleta;
import com.basesdatos2.backendBD2.modelo.Seguro;
import com.basesdatos2.backendBD2.modelo.emun.EstadoMotocicleta;
import com.basesdatos2.backendBD2.modelo.emun.EstadoPoliza;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

// Maneja MOTOCICLETA y su SEGURO (se registran y editan juntos).
@Repository
public class MotoRepository {
    private final JdbcTemplate jdbcTemplate;

    public MotoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String SQL_BASE = """
        SELECT Placa, Numero_documento, Marca, modelo, Color, Descripcion, `Año`,
               Cilindraje, Kilometraje_actual, Precio_alquiler, Estado_motocicleta, Id_seguro
        FROM MOTOCICLETA
        """;

    // El procedimiento no devuelve Color ni Descripcion; quedan en null.
    public List<Motocicleta> listarDisponibles(
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
        return jdbcTemplate.query("CALL sp_listar_motos_disponibles(?, ?)", (rs, fila) ->
                        new Motocicleta(
                                rs.getString("Placa").trim(),
                                rs.getInt("Documento_arrendador"),
                                rs.getString("Marca").trim(),
                                rs.getString("Modelo").trim(),
                                null,
                                null,
                                rs.getInt("Anio"),
                                rs.getBigDecimal("Cilindraje"),
                                rs.getInt("Kilometraje_actual"),
                                rs.getInt("Precio_alquiler"),
                                EstadoMotocicleta.valueOf(rs.getString("Estado_motocicleta").trim()),
                                rs.getInt("Id_seguro")
                        ),
                fechaInicio,
                fechaFin
        );
    }

    public List<Motocicleta> listarMias(Integer documentoArrendador) {
        return jdbcTemplate.query(
                SQL_BASE + """
                    WHERE Numero_documento = ?
                    ORDER BY Marca, modelo
                    """,
                this::mapear,
                documentoArrendador
        );
    }

    public Optional<Motocicleta> buscar(String placa, Integer documentoArrendador) {
        return jdbcTemplate.query(
                        SQL_BASE + "WHERE Placa = ? AND Numero_documento = ?",
                        this::mapear, placa, documentoArrendador)
                .stream()
                .findFirst();
    }

    public Optional<Seguro> buscarSeguro(Integer idSeguro) {
        String sql = """
            SELECT Id_seguro, numero_poliza, Aseguradora, Fecha_inicio_vigencia, Estado_poliza
            FROM SEGURO
            WHERE Id_seguro = ?
            """;

        return jdbcTemplate.query(sql, (rs, fila) ->
                        new Seguro(
                                rs.getInt("Id_seguro"),
                                rs.getInt("numero_poliza"),
                                rs.getString("Aseguradora").trim(),
                                rs.getDate("Fecha_inicio_vigencia").toLocalDate(),
                                EstadoPoliza.valueOf(rs.getString("Estado_poliza").trim())
                        ), idSeguro)
                .stream()
                .findFirst();
    }

    private Motocicleta mapear(ResultSet rs, int fila) throws SQLException {
        return new Motocicleta(
                rs.getString("Placa").trim(),
                rs.getInt("Numero_documento"),
                rs.getString("Marca").trim(),
                rs.getString("modelo").trim(),
                rs.getString("Color"),
                rs.getString("Descripcion"),
                rs.getInt("Año"),
                rs.getBigDecimal("Cilindraje"),
                rs.getInt("Kilometraje_actual"),
                rs.getInt("Precio_alquiler"),
                EstadoMotocicleta.valueOf(rs.getString("Estado_motocicleta").trim()),
                rs.getInt("Id_seguro")
        );
    }

    public Integer registrar(Motocicleta moto, Seguro seguro) {
        SimpleJdbcCall llamada = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_registrar_motocicleta")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_arrendador", Types.INTEGER),
                        new SqlParameter("p_placa", Types.CHAR),
                        new SqlParameter("p_marca", Types.CHAR),
                        new SqlParameter("p_modelo", Types.CHAR),
                        new SqlParameter("p_color", Types.CHAR),
                        new SqlParameter("p_descripcion", Types.VARCHAR),
                        new SqlParameter("p_anio", Types.INTEGER),
                        new SqlParameter("p_cilindraje", Types.DECIMAL),
                        new SqlParameter("p_kilometraje", Types.INTEGER),
                        new SqlParameter("p_precio_alquiler", Types.INTEGER),
                        new SqlParameter("p_numero_poliza", Types.INTEGER),
                        new SqlParameter("p_aseguradora", Types.CHAR),
                        new SqlParameter("p_fecha_inicio_vigencia", Types.DATE),
                        new SqlParameter("p_estado_poliza", Types.CHAR),
                        new SqlOutParameter("p_id_seguro", Types.INTEGER)
                );

        Map<String, Object> salida = llamada.execute(parametros(moto, seguro));

        return ((Number) salida.get("p_id_seguro")).intValue();
    }

    public void cambiarEstado(
            String placa,
            Integer documentoArrendador,
            EstadoMotocicleta estado) {

        SimpleJdbcCall llamada = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_cambiar_estado_motocicleta");

        llamada.execute(new MapSqlParameterSource()
                .addValue("p_placa", placa)
                .addValue("p_documento_arrendador", documentoArrendador)
                .addValue("p_estado", estado.name()));
    }

    public void actualizar(Motocicleta moto, Seguro seguro) {
        SimpleJdbcCall llamada = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_actualizar_motocicleta")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_arrendador", Types.INTEGER),
                        new SqlParameter("p_placa", Types.CHAR),
                        new SqlParameter("p_marca", Types.CHAR),
                        new SqlParameter("p_modelo", Types.CHAR),
                        new SqlParameter("p_color", Types.CHAR),
                        new SqlParameter("p_descripcion", Types.VARCHAR),
                        new SqlParameter("p_anio", Types.INTEGER),
                        new SqlParameter("p_cilindraje", Types.DECIMAL),
                        new SqlParameter("p_kilometraje", Types.INTEGER),
                        new SqlParameter("p_precio_alquiler", Types.INTEGER),
                        new SqlParameter("p_numero_poliza", Types.INTEGER),
                        new SqlParameter("p_aseguradora", Types.CHAR),
                        new SqlParameter("p_fecha_inicio_vigencia", Types.DATE),
                        new SqlParameter("p_estado_poliza", Types.CHAR)
                );

        llamada.execute(parametros(moto, seguro));
    }

    // Mismos parametros para registrar y actualizar.
    private MapSqlParameterSource parametros(Motocicleta moto, Seguro seguro) {
        return new MapSqlParameterSource()
                .addValue("p_arrendador", moto.getDocumentoArrendador())
                .addValue("p_placa", moto.getPlaca())
                .addValue("p_marca", moto.getMarca())
                .addValue("p_modelo", moto.getModelo())
                .addValue("p_color", moto.getColor())
                .addValue("p_descripcion", moto.getDescripcion())
                .addValue("p_anio", moto.getAnio())
                .addValue("p_cilindraje", moto.getCilindraje())
                .addValue("p_kilometraje", moto.getKilometrajeActual())
                .addValue("p_precio_alquiler", moto.getPrecioAlquiler())
                .addValue("p_numero_poliza", seguro.getNumeroPoliza())
                .addValue("p_aseguradora", seguro.getAseguradora())
                .addValue("p_fecha_inicio_vigencia", seguro.getFechaInicioVigencia())
                .addValue("p_estado_poliza", seguro.getEstadoPoliza().name());
    }
}
