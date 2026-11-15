package com.basesdatos2.backendBD2.repositorio;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.basesdatos2.backendBD2.modelo.Usuario;
import com.basesdatos2.backendBD2.modelo.emun.EstadoUsuario;
import com.basesdatos2.backendBD2.modelo.emun.Rol;
import com.basesdatos2.backendBD2.modelo.emun.TipoDocumento;

@Repository
public class UsuarioRepository {
    private final JdbcTemplate jdbcTemplate;

    public UsuarioRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String SQL_BASE = """
        SELECT Tipo_documento, Numero_documento, Primer_nombre, Segundo_nombre,
               Primer_apellido, Segundo_apellido, Telefono, Correo,
               Fecha_registro, Estado, `Contraseña`, Rol_usuario
        FROM USUARIO
        """;

    // La contrasena del usuario ya llega hasheada.
    public void registrar(Usuario usuario) {
        jdbcTemplate.update(
                "CALL sp_registrar_usuario(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                usuario.getTipoDocumento().name(),
                usuario.getNumeroDocumento(),
                usuario.getPrimerNombre(),
                usuario.getSegundoNombre(),
                usuario.getPrimerApellido(),
                usuario.getSegundoApellido(),
                usuario.getTelefono(),
                usuario.getCorreo(),
                usuario.getContrasena(),
                usuario.getRol().name()
        );
    }

    public Optional<Usuario> buscarPorCorreo(String correo) {
        List<Usuario> usuarios = jdbcTemplate.query(
                SQL_BASE + "WHERE Correo = ?", this::mapear, correo.trim());

        return usuarios.stream().findFirst();
    }

    public Optional<Usuario> buscarPorDocumento(Integer documento) {
        List<Usuario> usuarios = jdbcTemplate.query(
                SQL_BASE + "WHERE Numero_documento = ?", this::mapear, documento);

        return usuarios.stream().findFirst();
    }

    public boolean existeConRol(Rol rol) {
        Integer cantidad = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM USUARIO WHERE Rol_usuario = ?", Integer.class, rol.name());
        return cantidad != null && cantidad > 0;
    }

    public void actualizarContrasena(Integer numeroDocumento, String contrasenaHash) {
        jdbcTemplate.update(
                "UPDATE USUARIO SET `Contraseña` = ? WHERE Numero_documento = ?",
                contrasenaHash, numeroDocumento
        );
    }

    public List<Usuario> listarConContrasenaSinHashear() {
        return jdbcTemplate.query(
                SQL_BASE + "WHERE `Contraseña` NOT LIKE '$2a$%' AND `Contraseña` NOT LIKE '$2b$%' AND `Contraseña` NOT LIKE '$2y$%'",
                this::mapear
        );
    }

    public List<Usuario> listarTodos() {
        return jdbcTemplate.query(SQL_BASE + "ORDER BY Fecha_registro ASC, Numero_documento ASC", this::mapear);
    }

    public void asegurarEsquemaTelefonoBigInt() {
        jdbcTemplate.execute("ALTER TABLE USUARIO MODIFY COLUMN Telefono BIGINT NOT NULL COMMENT 'Telefono de contacto del usuario.'");
        jdbcTemplate.execute("DROP PROCEDURE IF EXISTS sp_registrar_usuario");
        jdbcTemplate.execute("""
                CREATE PROCEDURE sp_registrar_usuario(
                    IN p_tipo_documento CHAR(20),
                    IN p_numero_documento INT,
                    IN p_primer_nombre CHAR(50),
                    IN p_segundo_nombre CHAR(50),
                    IN p_primer_apellido CHAR(50),
                    IN p_segundo_apellido CHAR(50),
                    IN p_telefono BIGINT,
                    IN p_correo CHAR(50),
                    IN p_contrasena_hash VARCHAR(255),
                    IN p_rol CHAR(20)
                )
                BEGIN
                    DECLARE v_rol CHAR(20);
                    DECLARE e_llave_duplicada CONDITION FOR 1062;
                    DECLARE EXIT HANDLER FOR e_llave_duplicada
                    BEGIN
                        ROLLBACK;
                        SIGNAL SQLSTATE '45000'
                            SET MESSAGE_TEXT = 'Ya existe un usuario con ese numero de documento.', MYSQL_ERRNO = 45003;
                    END;
                    DECLARE EXIT HANDLER FOR SQLEXCEPTION
                    BEGIN
                        ROLLBACK;
                        RESIGNAL;
                    END;
                    SET v_rol = UPPER(TRIM(p_rol));
                    IF v_rol IS NULL OR v_rol NOT IN ('ADMIN', 'ARRENDADOR', 'ARRENDATARIO') THEN
                        SIGNAL SQLSTATE '45000'
                            SET MESSAGE_TEXT = 'Rol invalido (ADMIN, ARRENDADOR, ARRENDATARIO).', MYSQL_ERRNO = 45001;
                    END IF;
                    IF p_contrasena_hash IS NULL OR TRIM(p_contrasena_hash) = '' THEN
                        SIGNAL SQLSTATE '45000'
                            SET MESSAGE_TEXT = 'La contrasena (hash) es obligatoria.', MYSQL_ERRNO = 45001;
                    END IF;
                    START TRANSACTION;
                    INSERT INTO USUARIO
                        (Tipo_documento, Numero_documento, Primer_nombre, Segundo_nombre,
                         Primer_apellido, Segundo_apellido, Telefono, Correo,
                         Fecha_registro, Estado, `Contraseña`, Rol_usuario)
                    VALUES
                        (p_tipo_documento, p_numero_documento, p_primer_nombre, NULLIF(TRIM(p_segundo_nombre), ''),
                         p_primer_apellido, NULLIF(TRIM(p_segundo_apellido), ''), p_telefono, p_correo,
                         CURDATE(), 'ACTIVO', p_contrasena_hash, v_rol);
                    COMMIT;
                END
                """);
    }

    private Usuario mapear(ResultSet rs, int fila) throws SQLException {
        Usuario usuario = Usuario.segunRol(Rol.valueOf(rs.getString("Rol_usuario").trim()));
        usuario.setTipoDocumento(
                TipoDocumento.valueOf(rs.getString("Tipo_documento").trim()));
        usuario.setNumeroDocumento(rs.getInt("Numero_documento"));
        usuario.setPrimerNombre(rs.getString("Primer_nombre").trim());
        usuario.setSegundoNombre(textoOpcional(rs.getString("Segundo_nombre")));
        usuario.setPrimerApellido(rs.getString("Primer_apellido").trim());
        usuario.setSegundoApellido(textoOpcional(rs.getString("Segundo_apellido")));
        usuario.setTelefono(rs.getLong("Telefono"));
        usuario.setCorreo(rs.getString("Correo").trim());
        usuario.setFechaRegistro(rs.getDate("Fecha_registro").toLocalDate());
        usuario.setEstado(
                EstadoUsuario.valueOf(rs.getString("Estado").trim()));
        usuario.setContrasena(rs.getString("Contraseña"));
        return usuario;
    }

    private String textoOpcional(String valor) {
        return valor == null ? null : valor.trim();
    }
}
