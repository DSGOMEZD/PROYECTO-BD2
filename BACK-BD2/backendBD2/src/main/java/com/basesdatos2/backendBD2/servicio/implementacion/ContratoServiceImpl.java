package com.basesdatos2.backendBD2.servicio.implementacion;

import com.basesdatos2.backendBD2.modelo.Contrato;
import com.basesdatos2.backendBD2.modelo.Motocicleta;
import com.basesdatos2.backendBD2.modelo.Usuario;
import com.basesdatos2.backendBD2.modelo.dto.ContratoResponseDTO;
import com.basesdatos2.backendBD2.modelo.dto.MotoResumenDTO;
import com.basesdatos2.backendBD2.modelo.excepcion.RecursoNoEncontradoException;
import com.basesdatos2.backendBD2.repositorio.ContratoRepository;
import com.basesdatos2.backendBD2.repositorio.MotoRepository;
import com.basesdatos2.backendBD2.repositorio.UsuarioRepository;
import com.basesdatos2.backendBD2.servicio.ContratoService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContratoServiceImpl implements ContratoService {
    private final ContratoRepository contratoRepository;
    private final MotoRepository motoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModelMapper mm;

    public ContratoServiceImpl(ContratoRepository contratoRepository,
                               MotoRepository motoRepository,
                               UsuarioRepository usuarioRepository,
                               ModelMapper mm) {
        this.contratoRepository = contratoRepository;
        this.motoRepository = motoRepository;
        this.usuarioRepository = usuarioRepository;
        this.mm = mm;
    }

    @Override
    public List<ContratoResponseDTO> listarMios(Integer documentoArrendatario) {
        return contratoRepository.listarMios(documentoArrendatario).stream()
                .map(this::aDTO)
                .toList();
    }

    @Override
    public List<ContratoResponseDTO> listarComoArrendador(Integer documentoArrendador) {
        return contratoRepository.listarComoArrendador(documentoArrendador).stream()
                .map(this::aDTO)
                .toList();
    }

    @Override
    public ContratoResponseDTO buscarPorId(Integer idContrato) {
        return contratoRepository.buscarPorId(idContrato)
                .map(this::aDTO)
                .orElseThrow(() -> new RecursoNoEncontradoException("El contrato no existe."));
    }

    @Override
    public void cancelar(Integer idContrato, Integer documento) {
        contratoRepository.cancelar(idContrato, documento);
    }

    @Override
    public void finalizar(
            Integer idContrato,
            Integer documentoArrendador,
            Integer kilometrajeFinal
    ) {
        contratoRepository.finalizar(
                idContrato,
                documentoArrendador,
                kilometrajeFinal
        );
    }

    // Une el contrato con su moto y los nombres de las dos partes.
    private ContratoResponseDTO aDTO(Contrato contrato) {
        Motocicleta moto = motoRepository
                .buscar(contrato.getPlaca(), contrato.getDocumentoArrendador())
                .orElseThrow(() -> new RecursoNoEncontradoException("La motocicleta del contrato no existe."));

        ContratoResponseDTO dto = mm.map(contrato, ContratoResponseDTO.class);
        dto.setValorTotal(moto.valorAlquiler(contrato.getFechaInicio(), contrato.getFechaFin()));
        dto.setNombreArrendatario(nombreDe(contrato.getDocumentoArrendatario()));
        dto.setNombreArrendador(nombreDe(contrato.getDocumentoArrendador()));
        dto.setMoto(aResumen(moto));
        return dto;
    }

    private MotoResumenDTO aResumen(Motocicleta moto) {
        MotoResumenDTO resumen = mm.map(moto, MotoResumenDTO.class);
        resumen.setPrecioDia(moto.getPrecioAlquiler());
        return resumen;
    }

    private String nombreDe(Integer documento) {
        return usuarioRepository.buscarPorDocumento(documento)
                .map(Usuario::nombreCompleto)
                .orElse(null);
    }
}
