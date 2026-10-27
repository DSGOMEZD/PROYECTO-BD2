package com.basesdatos2.backendBD2.servicio;

import com.basesdatos2.backendBD2.modelo.dto.LoginRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.RegistroRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.UsuarioResponseDTO;

public interface AuthService {
    void registrar(RegistroRequestDTO dto);
    UsuarioResponseDTO iniciarSesion(LoginRequestDTO dto);
}
