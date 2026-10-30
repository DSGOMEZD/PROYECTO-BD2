package com.basesdatos2.backendBD2.modelo;

import com.basesdatos2.backendBD2.modelo.emun.Rol;

// USUARIO con Rol_usuario = ADMIN
public class Administrador extends Usuario {

    public Administrador() {
        super(Rol.ADMIN);
    }
}
