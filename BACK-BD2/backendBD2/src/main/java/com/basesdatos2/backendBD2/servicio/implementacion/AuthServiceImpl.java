package com.basesdatos2.backendBD2.servicio.implementacion;

import com.basesdatos2.backendBD2.modelo.Usuario;
import com.basesdatos2.backendBD2.modelo.dto.LoginRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.RegistroRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.UsuarioResponseDTO;
import com.basesdatos2.backendBD2.modelo.emun.EstadoUsuario;
import com.basesdatos2.backendBD2.modelo.emun.Rol;
import com.basesdatos2.backendBD2.modelo.excepcion.NoAutorizadoException;
import com.basesdatos2.backendBD2.repositorio.UsuarioRepository;
import com.basesdatos2.backendBD2.servicio.AuthService;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthServiceImpl implements AuthService {
    private final UsuarioRepository usuarioRepository;
    private final ModelMapper mm;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           ModelMapper mm,
                           BCryptPasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.mm = mm;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void registrar(RegistroRequestDTO dto) {
        // El administrador no se registra desde la app: se crea al arrancar el back.
        if (dto.rol() == Rol.ADMIN) {
            throw new NoAutorizadoException("No es posible registrarse como administrador.");
        }
        Usuario usuario = new Usuario();
        mm.map(dto, usuario);
        usuario.setRol(dto.rol());
        usuario.setContrasena(passwordEncoder.encode(dto.contrasena()));
        usuarioRepository.registrar(usuario);
    }

    @Override
    public UsuarioResponseDTO iniciarSesion(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepository.buscarPorCorreo(dto.correo())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos."));

        if (usuario.getEstado() != EstadoUsuario.ACTIVO
                || !validarContrasena(dto.contrasena(), usuario)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.");
        }

        UsuarioResponseDTO respuesta = mm.map(usuario, UsuarioResponseDTO.class);
        respuesta.setNombre(usuario.nombres());
        respuesta.setApellido(usuario.apellidos());
        return respuesta;
    }

    private boolean validarContrasena(String contrasenaIngresada, Usuario usuario) {
        String almacenada = usuario.getContrasena();
        if (almacenada == null || contrasenaIngresada == null) {
            return false;
        }
        if (esHashBCrypt(almacenada)) {
            return passwordEncoder.matches(contrasenaIngresada, almacenada);
        }
        // Si un registro fue insertado manualmente en texto plano en MySQL,
        // valida una vez y lo migra inmediatamente a hash BCrypt irreversible.
        if (contrasenaIngresada.equals(almacenada)) {
            String nuevoHash = passwordEncoder.encode(contrasenaIngresada);
            usuarioRepository.actualizarContrasena(usuario.getNumeroDocumento(), nuevoHash);
            usuario.setContrasena(nuevoHash);
            return true;
        }
        return false;
    }

    private boolean esHashBCrypt(String valor) {
        return valor.startsWith("$2a$") || valor.startsWith("$2b$") || valor.startsWith("$2y$");
    }
}
