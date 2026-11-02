package com.basesdatos2.backendBD2.servicio;

import com.basesdatos2.backendBD2.modelo.dto.LogResponseDTO;
import com.basesdatos2.backendBD2.modelo.emun.ModuloLog;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public interface LogService {

    // Guarda un evento en MongoDB. Nunca lanza excepcion: si Mongo falla,
    // la operacion de negocio (que ya se hizo en MySQL) no se ve afectada.
    void registrar(ModuloLog modulo,
                   String accion,
                   Integer documentoUsuario,
                   String referencia,
                   String descripcion,
                   Map<String, Object> detalle);

    // Solo el usuario con rol ADMIN puede consultar los logs.
    List<LogResponseDTO> listar(Integer documentoAdmin,
                                LocalDate desde,
                                LocalDate hasta,
                                ModuloLog modulo);

    // Ayudas para armar las descripciones legibles de los eventos.
    static String pesos(Integer valor) {
        if (valor == null) return "$0";
        NumberFormat formato = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO"));
        return "$" + formato.format(valor);
    }

    static String fecha(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
