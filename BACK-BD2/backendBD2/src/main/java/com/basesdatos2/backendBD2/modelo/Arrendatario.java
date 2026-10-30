package com.basesdatos2.backendBD2.modelo;

import com.basesdatos2.backendBD2.modelo.emun.Rol;

// USUARIO con Rol_usuario = ARRENDATARIO: quien alquila motocicletas
public class Arrendatario extends Usuario {

    public Arrendatario() {
        super(Rol.ARRENDATARIO);
    }
}
