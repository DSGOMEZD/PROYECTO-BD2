package com.basesdatos2.backendBD2.modelo.excepcion;

import org.springframework.http.HttpStatus;

import java.sql.SQLException;

public class MapeoErroresSQL {
    public static SQLException buscarErrorNegocio(Throwable ex) {
        while (ex != null) {
            if (ex instanceof SQLException sql
                    && sql.getErrorCode() >= 45001 && sql.getErrorCode() <= 45010) {
                return sql;
            }
            ex = ex.getCause();
        }
        return null;
    }

    public static HttpStatus aHttp(int codigo) {
        return switch (codigo) {
            case 45003, 45005, 45008, 45009 -> HttpStatus.CONFLICT;
            case 45004 -> HttpStatus.NOT_FOUND;
            case 45007 -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
