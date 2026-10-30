package com.basesdatos2.backendBD2.modelo;

import com.basesdatos2.backendBD2.modelo.emun.Rol;

// USUARIO con Rol_usuario = ARRENDADOR: dueño de motocicletas
public class Arrendador extends Usuario {

    public Arrendador() {
        super(Rol.ARRENDADOR);
    }
}
