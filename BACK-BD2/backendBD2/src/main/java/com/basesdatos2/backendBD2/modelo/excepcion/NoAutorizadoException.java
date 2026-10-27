package com.basesdatos2.backendBD2.modelo.excepcion;

public class NoAutorizadoException extends RuntimeException {
    public NoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
