package com.basesdatos2.backendBD2.controlador;

import java.sql.SQLException;

import com.basesdatos2.backendBD2.modelo.excepcion.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.basesdatos2.backendBD2.modelo.dto.ErrorResponseDTO;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponseDTO> reglaNegocio(ReglaNegocioException ex) {
        return ResponseEntity.status(MapeoErroresSQL.aHttp(ex.getCodigo()))
                .body(new ErrorResponseDTO(ex.getCodigo(), ex.getMessage()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponseDTO> noEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDTO(45004, ex.getMessage()));
    }

    @ExceptionHandler(NoAutorizadoException.class)
    public ResponseEntity<ErrorResponseDTO> noAutorizado(NoAutorizadoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponseDTO(45007, ex.getMessage()));
    }


    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponseDTO> errorBaseDatos(DataAccessException ex) {

        log.error("Error al consultar la base de datos", ex);

        SQLException sql = MapeoErroresSQL.buscarErrorNegocio(ex);
        if (sql != null) {
            return ResponseEntity.status(MapeoErroresSQL.aHttp(sql.getErrorCode()))
                    .body(new ErrorResponseDTO(sql.getErrorCode(), sql.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDTO(500, "Error interno de la base de datos."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> validacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("Datos inválidos.");
        return ResponseEntity.badRequest().body(new ErrorResponseDTO(45001, mensaje));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> formatoInvalido(HttpMessageNotReadableException ex) {
        log.warn("Formato de datos inválido en la petición: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(
                new ErrorResponseDTO(45001, "Revisa los datos numéricos o el formato de los campos ingresados."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDTO> responseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(new ErrorResponseDTO(ex.getStatusCode().value(),
                        ex.getReason() != null ? ex.getReason() : "Error en la solicitud."));
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponseDTO> credencialesInvalidas(
            CredencialesInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponseDTO(401, ex.getMessage()));
    }
}
