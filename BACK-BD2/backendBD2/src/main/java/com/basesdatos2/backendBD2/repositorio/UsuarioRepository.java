package com.basesdatos2.backendBD2.repositorio;
import com.basesdatos2.backendBD2.modelo.dto.RegistroRequestDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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

    public void registrar(RegistroRequestDTO dto, String contrasenaHash) {
        jdbcTemplate.update(
                "CALL sp_registrar_usuario(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                dto.tipoDocumento().name(),
                dto.numeroDocumento(),
                dto.primerNombre(),
                dto.segundoNombre(),
                dto.primerApellido(),
                dto.segundoApellido(),
                dto.telefono(),
                dto.correo(),
                contrasenaHash,
                dto.rol().name()
        );
    }

    public Optional<Usuario> buscarPorCorreo(String correo) {
        String sql = """
        SELECT Tipo_documento, Numero_documento, Primer_nombre, Segundo_nombre,
               Primer_apellido, Segundo_apellido, Telefono, Correo,
               Fecha_registro, Estado, `Contraseña`, Rol_usuario
        FROM USUARIO
        WHERE Correo = ?
        """;

        List<Usuario> usuarios = jdbcTemplate.query(sql, (rs, fila) -> {
            Usuario usuario = new Usuario();
            usuario.setTipoDocumento(
                    TipoDocumento.valueOf(rs.getString("Tipo_documento").trim()));
            usuario.setNumeroDocumento(rs.getInt("Numero_documento"));
            usuario.setPrimerNombre(rs.getString("Primer_nombre").trim());
            usuario.setSegundoNombre(rs.getString("Segundo_nombre").trim());
            usuario.setPrimerApellido(rs.getString("Primer_apellido").trim());
            usuario.setSegundoApellido(rs.getString("Segundo_apellido").trim());
            usuario.setTelefono(rs.getInt("Telefono"));
            usuario.setCorreo(rs.getString("Correo").trim());
            usuario.setFechaRegistro(rs.getDate("Fecha_registro").toLocalDate());
            usuario.setEstado(
                    EstadoUsuario.valueOf(rs.getString("Estado").trim()));
            usuario.setContrasena(rs.getString("Contraseña"));
            usuario.setRol(Rol.valueOf(rs.getString("Rol_usuario").trim()));
            return usuario;
        }, correo.trim());

        return usuarios.stream().findFirst();
    }
}
