package com.basesdatos2.backendBD2.repositorio;
import java.time.LocalDate;
import java.util.List;

import com.basesdatos2.backendBD2.modelo.dto.MotocicletaResponseDTO;
import com.basesdatos2.backendBD2.modelo.dto.SeguroResponseDTO;
import com.basesdatos2.backendBD2.modelo.emun.EstadoMotocicleta;
import com.basesdatos2.backendBD2.modelo.emun.EstadoPoliza;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.Map;

import com.basesdatos2.backendBD2.modelo.dto.MotocicletaRequestDTO;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

@Repository
public class MotoRepository {
    private final JdbcTemplate jdbcTemplate;

    public MotoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MotocicletaResponseDTO> listarDisponibles(
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
        String sql = "CALL sp_listar_motos_disponibles(?, ?)";

        return jdbcTemplate.query(sql, (rs, fila) ->
                        new MotocicletaResponseDTO(
                                rs.getString("Placa").trim(),
                                rs.getString("Marca").trim(),
                                rs.getString("Modelo").trim(),
                                rs.getString("Color"),
                                rs.getString("Descripcion"),
                                rs.getInt("Anio"),
                                rs.getBigDecimal("Cilindraje"),
                                rs.getInt("Kilometraje_actual"),
                                rs.getInt("Precio_alquiler"),
                                EstadoMotocicleta.valueOf(
                                        rs.getString("Estado_motocicleta").trim()),
                                rs.getInt("Documento_arrendador"),
                                rs.getString("Nombre_arrendador").trim(),
                                new SeguroResponseDTO(
                                        rs.getInt("Id_seguro"),
                                        rs.getInt("Numero_poliza"),
                                        rs.getString("Aseguradora").trim(),
                                        rs.getDate("Fecha_inicio_vigencia").toLocalDate(),
                                        EstadoPoliza.valueOf(rs.getString("Estado_poliza").trim())
                                ),// El procedimiento no devuelve el seguro.
                                rs.getInt("Valor_estimado")
                        ),
                fechaInicio,
                fechaFin
        );
    }

    public Integer registrar(Integer documentoArrendador, MotocicletaRequestDTO dto) {
        var seguro = dto.seguro();

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

        Map<String, Object> salida = llamada.execute(new MapSqlParameterSource()
                .addValue("p_arrendador", documentoArrendador)
                .addValue("p_placa", dto.placa())
                .addValue("p_marca", dto.marca())
                .addValue("p_modelo", dto.modelo())
                .addValue("p_color", dto.color())
                .addValue("p_descripcion", dto.descripcion())
                .addValue("p_anio", dto.anio())
                .addValue("p_cilindraje", dto.cilindraje())
                .addValue("p_kilometraje", dto.kilometraje())
                .addValue("p_precio_alquiler", dto.precioDia())
                .addValue("p_numero_poliza", seguro.numeroPoliza())
                .addValue("p_aseguradora", seguro.aseguradora())
                .addValue("p_fecha_inicio_vigencia", seguro.fechaInicioVigencia())
                .addValue("p_estado_poliza", seguro.estadoPoliza().name()));

        return ((Number) salida.get("p_id_seguro")).intValue();
    }

    public List<MotocicletaResponseDTO> listarMias(Integer documentoArrendador) {
        String sql = """
        SELECT
            m.Placa,
            m.Marca,
            m.modelo AS Modelo,
            m.`Año` AS Anio,
            m.Cilindraje,
            m.Kilometraje_actual,
            m.Precio_alquiler,
            m.Estado_motocicleta,
            m.Numero_documento AS Documento_arrendador,
            m.Color,
            m.Descripcion,
            fn_nombre_completo(m.Numero_documento) AS Nombre_arrendador,
            s.Id_seguro,
            s.numero_poliza AS Numero_poliza,
            s.Aseguradora,
            s.Fecha_inicio_vigencia,
            s.Estado_poliza
        FROM MOTOCICLETA m
        JOIN SEGURO s ON s.Id_seguro = m.Id_seguro
        WHERE m.Numero_documento = ?
        ORDER BY m.Marca, m.modelo
        """;

        return jdbcTemplate.query(sql, (rs, fila) -> {
            SeguroResponseDTO seguro = new SeguroResponseDTO(
                    rs.getInt("Id_seguro"),
                    rs.getInt("Numero_poliza"),
                    rs.getString("Aseguradora").trim(),
                    rs.getDate("Fecha_inicio_vigencia").toLocalDate(),
                    EstadoPoliza.valueOf(rs.getString("Estado_poliza").trim())
            );

            return new MotocicletaResponseDTO(
                    rs.getString("Placa").trim(),
                    rs.getString("Marca").trim(),
                    rs.getString("Modelo").trim(),
                    rs.getString("Color"),
                    rs.getString("Descripcion"),
                    rs.getInt("Anio"),
                    rs.getBigDecimal("Cilindraje"),
                    rs.getInt("Kilometraje_actual"),
                    rs.getInt("Precio_alquiler"),
                    EstadoMotocicleta.valueOf(
                            rs.getString("Estado_motocicleta").trim()
                    ),
                    rs.getInt("Documento_arrendador"),
                    rs.getString("Nombre_arrendador").trim(),
                    seguro,
                    null
            );
        }, documentoArrendador);
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

}
