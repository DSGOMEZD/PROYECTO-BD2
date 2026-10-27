package com.basesdatos2.backendBD2.servicio.implementacion;

import com.basesdatos2.backendBD2.modelo.Usuario;
import com.basesdatos2.backendBD2.modelo.dto.LoginRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.RegistroRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.UsuarioResponseDTO;
import com.basesdatos2.backendBD2.modelo.emun.EstadoUsuario;
import com.basesdatos2.backendBD2.repositorio.UsuarioRepository;
import com.basesdatos2.backendBD2.servicio.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AuthServiceImpl implements AuthService {
    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void registrar(RegistroRequestDTO dto) {
        String hash = passwordEncoder.encode(dto.contrasena());
        usuarioRepository.registrar(dto, hash);
    }
    @Override
    public UsuarioResponseDTO iniciarSesion(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepository.buscarPorCorreo(dto.correo())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos."));

        if (usuario.getEstado() != EstadoUsuario.ACTIVO
                || !passwordEncoder.matches(dto.contrasena(), usuario.getContrasena())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.");
        }

        String nombre = Stream.of(usuario.getPrimerNombre(), usuario.getSegundoNombre())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" "));

        String apellido = Stream.of(usuario.getPrimerApellido(), usuario.getSegundoApellido())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" "));

        return new UsuarioResponseDTO(
                usuario.getNumeroDocumento(),
                usuario.getTipoDocumento(),
                nombre,
                apellido,
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getRol(),
                usuario.getEstado()
        );
    }
}
