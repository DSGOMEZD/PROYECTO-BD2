package com.basesdatos2.backendBD2.servicio;

import com.basesdatos2.backendBD2.modelo.dto.PagoResponseDTO;
import com.basesdatos2.backendBD2.modelo.emun.MetodoPago;

import java.util.List;

public interface PagoService {
    List<PagoResponseDTO> listarMios(Integer documentoArrendatario);

    List<PagoResponseDTO> listarRecibidos(Integer documentoArrendador);

    void registrar(Integer idPago, Integer documentoArrendatario, MetodoPago metodoPago);
}
