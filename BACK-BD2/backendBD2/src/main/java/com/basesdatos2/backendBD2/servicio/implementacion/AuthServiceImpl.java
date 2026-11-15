package com.basesdatos2.backendBD2.servicio.implementacion;

import com.basesdatos2.backendBD2.modelo.Usuario;
import com.basesdatos2.backendBD2.modelo.dto.LoginRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.RegistroRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.UsuarioResponseDTO;
import com.basesdatos2.backendBD2.modelo.emun.EstadoUsuario;
import com.basesdatos2.backendBD2.modelo.emun.ModuloLog;
import com.basesdatos2.backendBD2.modelo.emun.Rol;
import com.basesdatos2.backendBD2.modelo.excepcion.CredencialesInvalidasException;
import com.basesdatos2.backendBD2.modelo.excepcion.NoAutorizadoException;
import com.basesdatos2.backendBD2.repositorio.UsuarioRepository;
import com.basesdatos2.backendBD2.servicio.AuthService;
import com.basesdatos2.backendBD2.servicio.LogService;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {
    private final UsuarioRepository usuarioRepository;
    private final LogService logService;
    private final ModelMapper mm;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           LogService logService,
                           ModelMapper mm,
                           BCryptPasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.logService = logService;
        this.mm = mm;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void registrar(RegistroRequestDTO dto) {
        // El administrador no se registra desde la app: se crea al arrancar el back.
        if (dto.rol() == Rol.ADMIN) {
            throw new NoAutorizadoException("No es posible registrarse como administrador.");
        }
        Usuario usuario = Usuario.segunRol(dto.rol());
        mm.map(dto, usuario);
        normalizarNombresYApellidos(usuario);
        usuario.setContrasena(passwordEncoder.encode(dto.contrasena()));
        usuarioRepository.registrar(usuario);

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("tipoDocumento", usuario.getTipoDocumento());
        detalle.put("numeroDocumento", usuario.getNumeroDocumento());
        detalle.put("nombreCompleto", usuario.nombreCompleto());
        detalle.put("correo", usuario.getCorreo());
        detalle.put("telefono", usuario.getTelefono());
        detalle.put("rol", usuario.getRol());

        logService.registrar(
                ModuloLog.USUARIO,
                "REGISTRADO",
                usuario.getNumeroDocumento(),
                "Usuario #" + usuario.getNumeroDocumento(),
                "Se registró el usuario " + usuario.nombreCompleto() + " (" + usuario.getCorreo() + ") con rol " + usuario.getRol() + ".",
                detalle
        );
    }

    @Override
    public UsuarioResponseDTO iniciarSesion(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepository.buscarPorCorreo(dto.correo())
                .orElseThrow(() -> {
                    registrarIntentoFallido(dto.correo(), null, "Correo no registrado");
                    return new CredencialesInvalidasException();
                });

        if (usuario.getEstado() != EstadoUsuario.ACTIVO
                || !validarContrasena(dto.contrasena(), usuario)) {
            registrarIntentoFallido(dto.correo(), usuario.getNumeroDocumento(), "Credenciales inválidas o usuario inactivo");
            throw new CredencialesInvalidasException();
        }

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("numeroDocumento", usuario.getNumeroDocumento());
        detalle.put("nombreCompleto", usuario.nombreCompleto());
        detalle.put("correo", usuario.getCorreo());
        detalle.put("rol", usuario.getRol());
        detalle.put("estado", usuario.getEstado());

        logService.registrar(
                ModuloLog.USUARIO,
                "SESION",
                usuario.getNumeroDocumento(),
                usuario.getCorreo(),
                "Inició sesión " + usuario.nombreCompleto() + " (" + usuario.getRol() + ").",
                detalle
        );

        UsuarioResponseDTO respuesta = mm.map(usuario, UsuarioResponseDTO.class);
        respuesta.setNombre(usuario.nombres());
        respuesta.setApellido(usuario.apellidos());
        return respuesta;
    }

    private void registrarIntentoFallido(String correo, Integer documento, String motivo) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("correoIntentado", correo);
        detalle.put("motivo", motivo);
        logService.registrar(
                ModuloLog.USUARIO,
                "FALLIDO",
                documento,
                correo,
                "Intento fallido de inicio de sesión para el correo " + correo + ".",
                detalle
        );
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

    private void normalizarNombresYApellidos(Usuario usuario) {
        String[] nombres = separarPrimeroYSegundo(usuario.getPrimerNombre(), usuario.getSegundoNombre());
        usuario.setPrimerNombre(nombres[0]);
        usuario.setSegundoNombre(nombres[1]);

        String[] apellidos = separarPrimeroYSegundo(usuario.getPrimerApellido(), usuario.getSegundoApellido());
        usuario.setPrimerApellido(apellidos[0]);
        usuario.setSegundoApellido(apellidos[1]);
    }

    private String[] separarPrimeroYSegundo(String primeroActual, String segundoActual) {
        String p1 = primeroActual != null ? primeroActual.trim().replaceAll("\\s+", " ") : "";
        String p2 = (segundoActual != null && !segundoActual.isBlank())
                ? segundoActual.trim().replaceAll("\\s+", " ")
                : null;

        if (p2 == null && p1.contains(" ")) {
            int idx = p1.indexOf(' ');
            return new String[]{p1.substring(0, idx).trim(), p1.substring(idx + 1).trim()};
        }
        return new String[]{p1, p2};
    }

    private boolean esHashBCrypt(String valor) {
        return valor.startsWith("$2a$") || valor.startsWith("$2b$") || valor.startsWith("$2y$");
    }
}
