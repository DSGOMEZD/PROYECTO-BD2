package com.basesdatos2.backendBD2.servicio.implementacion;

import com.basesdatos2.backendBD2.modelo.Usuario;
import com.basesdatos2.backendBD2.modelo.dto.LoginRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.RegistroRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.UsuarioResponseDTO;
import com.basesdatos2.backendBD2.modelo.emun.EstadoUsuario;
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
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthServiceImpl(UsuarioRepository usuarioRepository, ModelMapper mm) {
        this.usuarioRepository = usuarioRepository;
        this.mm = mm;
    }

    @Override
    public void registrar(RegistroRequestDTO dto) {
        Usuario usuario = Usuario.segunRol(dto.rol());
        mm.map(dto, usuario);
        usuario.setContrasena(passwordEncoder.encode(dto.contrasena()));
        usuarioRepository.registrar(usuario);
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

        UsuarioResponseDTO respuesta = mm.map(usuario, UsuarioResponseDTO.class);
        respuesta.setNombre(usuario.nombres());
        respuesta.setApellido(usuario.apellidos());
        return respuesta;
    }
}
