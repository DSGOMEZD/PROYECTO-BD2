package com.basesdatos2.backendBD2.servicio.implementacion;

import com.basesdatos2.backendBD2.modelo.dto.PagoResponseDTO;
import com.basesdatos2.backendBD2.modelo.emun.MetodoPago;
import com.basesdatos2.backendBD2.repositorio.PagoRepository;
import com.basesdatos2.backendBD2.servicio.PagoService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PagoServiceImpl implements PagoService {
    private final PagoRepository pagoRepository;

    public PagoServiceImpl(PagoRepository pagoRepository) {
        this.pagoRepository = pagoRepository;
    }

    @Override
    public List<PagoResponseDTO> listarMios(Integer documentoArrendatario) {
        return pagoRepository.listarMios(documentoArrendatario);
    }

    @Override
    public List<PagoResponseDTO> listarRecibidos(Integer documentoArrendador) {
        return pagoRepository.listarRecibidos(documentoArrendador);
    }

    @Override
    public void registrar(Integer idPago, Integer documentoArrendatario, MetodoPago metodoPago) {
        pagoRepository.registrar(idPago, documentoArrendatario, metodoPago);
    }
}