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

    private Usuario mapear(ResultSet rs, int fila) throws SQLException {
        Usuario usuario = Usuario.segunRol(Rol.valueOf(rs.getString("Rol_usuario").trim()));
        usuario.setTipoDocumento(
                TipoDocumento.valueOf(rs.getString("Tipo_documento").trim()));
        usuario.setNumeroDocumento(rs.getInt("Numero_documento"));
        usuario.setPrimerNombre(rs.getString("Primer_nombre").trim());
        usuario.setSegundoNombre(textoOpcional(rs.getString("Segundo_nombre")));
        usuario.setPrimerApellido(rs.getString("Primer_apellido").trim());
        usuario.setSegundoApellido(textoOpcional(rs.getString("Segundo_apellido")));
        usuario.setTelefono(rs.getInt("Telefono"));
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
