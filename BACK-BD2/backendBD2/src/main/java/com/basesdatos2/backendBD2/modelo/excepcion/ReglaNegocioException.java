package com.basesdatos2.backendBD2.modelo.excepcion;

public class ReglaNegocioException extends RuntimeException {
    private final int codigo;

    public ReglaNegocioException(int codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public int getCodigo() { return codigo; }
}
