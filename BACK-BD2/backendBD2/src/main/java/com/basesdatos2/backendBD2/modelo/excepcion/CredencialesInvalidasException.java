package com.basesdatos2.backendBD2.modelo.excepcion;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos.");
    }
}