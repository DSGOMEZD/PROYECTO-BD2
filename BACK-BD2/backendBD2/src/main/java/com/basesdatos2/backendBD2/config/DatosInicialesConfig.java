package com.basesdatos2.backendBD2.config;

import com.basesdatos2.backendBD2.modelo.Usuario;
import com.basesdatos2.backendBD2.modelo.emun.Rol;
import com.basesdatos2.backendBD2.modelo.emun.TipoDocumento;
import com.basesdatos2.backendBD2.repositorio.LogRepository;
import com.basesdatos2.backendBD2.repositorio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

// Se ejecuta una vez al arrancar el back (en segundo plano para no bloquear el arranque):
//  1) crea los indices de la coleccion de logs en MongoDB;
//  2) crea el usuario ADMIN (el unico que ve los logs) si todavia no existe ninguno.
@Component
public class DatosInicialesConfig implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DatosInicialesConfig.class);

    private final LogRepository logRepository;
    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Integer documentoAdmin;
    private final String correoAdmin;
    private final String contrasenaAdmin;

    public DatosInicialesConfig(LogRepository logRepository,
                                UsuarioRepository usuarioRepository,
                                BCryptPasswordEncoder passwordEncoder,
                                @Value("${app.admin.documento}") Integer documentoAdmin,
                                @Value("${app.admin.correo}") String correoAdmin,
                                @Value("${app.admin.contrasena}") String contrasenaAdmin) {
        this.logRepository = logRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.documentoAdmin = documentoAdmin;
        this.correoAdmin = correoAdmin;
        this.contrasenaAdmin = contrasenaAdmin;
    }

    @Async
    @Override
    public void run(ApplicationArguments args) {
        try {
            logRepository.crearIndices();
        } catch (RuntimeException e) {
            log.warn("No se pudieron crear los indices de logs en MongoDB: {}", e.getMessage());
        }

        try {
            if (!usuarioRepository.existeConRol(Rol.ADMIN)) {
                usuarioRepository.registrar(adminInicial());
                log.info("Usuario ADMIN creado con hash BCrypt: {}", correoAdmin);
            }
            migrarContrasenasEnTextoPlano();
        } catch (RuntimeException e) {
            log.warn("No se pudo verificar/crear el usuario ADMIN: {}", e.getMessage());
        }
    }

    private void migrarContrasenasEnTextoPlano() {
        for (Usuario u : usuarioRepository.listarConContrasenaSinHashear()) {
            if (u.getContrasena() != null && !u.getContrasena().isBlank()) {
                usuarioRepository.actualizarContrasena(
                        u.getNumeroDocumento(),
                        passwordEncoder.encode(u.getContrasena()));
                log.info("Contraseña en texto plano migrada a BCrypt para el usuario {}", u.getCorreo());
            }
        }
    }

    private Usuario adminInicial() {
        Usuario admin = Usuario.segunRol(Rol.ADMIN);
        admin.setTipoDocumento(TipoDocumento.CC);
        admin.setNumeroDocumento(documentoAdmin);
        admin.setPrimerNombre("Administrador");
        admin.setPrimerApellido("MotoGo");
        admin.setTelefono(1);
        admin.setCorreo(correoAdmin);
        admin.setContrasena(passwordEncoder.encode(contrasenaAdmin));
        return admin;
    }
}
