package com.basesdatos2.backendBD2.Modelo.dto;

import com.basesdatos2.backendBD2.Modelo.emun.EstadoUsuario;
import com.basesdatos2.backendBD2.Modelo.emun.Rol;
import com.basesdatos2.backendBD2.Modelo.emun.TipoDocumento;

// Sin contrasena: nunca viaja de vuelta al front.
// nombre = primer + segundo nombre; apellido = primer + segundo apellido.
public record UsuarioResponseDTO(
        Integer numeroDocumento,
        TipoDocumento tipoDocumento,
        String nombre,
        String apellido,
        String correo,
        Integer telefono,
        Rol rol,
        EstadoUsuario estado
) {}
