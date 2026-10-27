package com.basesdatos2.backendBD2.controlador;

import com.basesdatos2.backendBD2.modelo.dto.LoginRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.RegistroRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.UsuarioResponseDTO;
import com.basesdatos2.backendBD2.servicio.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public void registrar(@Valid @RequestBody RegistroRequestDTO dto) {
        authService.registrar(dto);
    }

    @PostMapping("/login")
    public UsuarioResponseDTO iniciarSesion(@Valid @RequestBody LoginRequestDTO dto) {
        return authService.iniciarSesion(dto);
    }
}
