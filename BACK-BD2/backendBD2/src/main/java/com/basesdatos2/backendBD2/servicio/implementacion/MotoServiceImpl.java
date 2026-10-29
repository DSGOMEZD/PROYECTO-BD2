package com.basesdatos2.backendBD2.servicio.implementacion;

import com.basesdatos2.backendBD2.modelo.Motocicleta;
import com.basesdatos2.backendBD2.modelo.Seguro;
import com.basesdatos2.backendBD2.modelo.Usuario;
import com.basesdatos2.backendBD2.modelo.dto.MotocicletaRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.MotocicletaResponseDTO;
import com.basesdatos2.backendBD2.modelo.dto.SeguroResponseDTO;
import com.basesdatos2.backendBD2.modelo.emun.EstadoMotocicleta;
import com.basesdatos2.backendBD2.repositorio.MotoRepository;
import com.basesdatos2.backendBD2.repositorio.UsuarioRepository;
import com.basesdatos2.backendBD2.servicio.MotoService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class MotoServiceImpl implements MotoService {
    private final MotoRepository motoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModelMapper mm;

    public MotoServiceImpl(MotoRepository motoRepository,
                           UsuarioRepository usuarioRepository,
                           ModelMapper mm) {
        this.motoRepository = motoRepository;
        this.usuarioRepository = usuarioRepository;
        this.mm = mm;
    }

    // El catalogo no incluye el seguro, pero si el valor estimado del alquiler.
    @Override
    public List<MotocicletaResponseDTO> listarDisponibles(
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
        return motoRepository.listarDisponibles(fechaInicio, fechaFin).stream()
                .map(moto -> {
                    MotocicletaResponseDTO dto = aDTO(moto);
                    dto.setValorEstimado(moto.valorAlquiler(fechaInicio, fechaFin));
                    return dto;
                })
                .toList();
    }

    @Override
    public Integer registrar(Integer documentoArrendador, MotocicletaRequestDTO dto) {
        return motoRepository.registrar(aEntidad(documentoArrendador, dto), mm.map(dto.seguro(), Seguro.class));
    }

    @Override
    public List<MotocicletaResponseDTO> listarMias(Integer documentoArrendador) {
        return motoRepository.listarMias(documentoArrendador).stream()
                .map(moto -> {
                    MotocicletaResponseDTO dto = aDTO(moto);
                    motoRepository.buscarSeguro(moto.getIdSeguro())
                            .ifPresent(s -> dto.setSeguro(mm.map(s, SeguroResponseDTO.class)));
                    return dto;
                })
                .toList();
    }

    @Override
    public void cambiarEstado(
            String placa,
            Integer documentoArrendador,
            EstadoMotocicleta estado) {
        motoRepository.cambiarEstado(placa, documentoArrendador, estado);
    }

    @Override
    public void actualizar(
            String placa,
            Integer documentoArrendador,
            MotocicletaRequestDTO dto
    ) {
        Motocicleta moto = aEntidad(documentoArrendador, dto);
        moto.setPlaca(placa);
        motoRepository.actualizar(moto, mm.map(dto.seguro(), Seguro.class));
    }

    // Campos con el mismo nombre los copia ModelMapper; los demas se llenan a mano.
    private Motocicleta aEntidad(Integer documentoArrendador, MotocicletaRequestDTO dto) {
        Motocicleta moto = mm.map(dto, Motocicleta.class);
        moto.setDocumentoArrendador(documentoArrendador);
        moto.setKilometrajeActual(dto.kilometraje());
        moto.setPrecioAlquiler(dto.precioDia());
        return moto;
    }

    private MotocicletaResponseDTO aDTO(Motocicleta moto) {
        MotocicletaResponseDTO dto = mm.map(moto, MotocicletaResponseDTO.class);
        dto.setKilometraje(moto.getKilometrajeActual());
        dto.setPrecioDia(moto.getPrecioAlquiler());
        dto.setNombreArrendador(usuarioRepository.buscarPorDocumento(moto.getDocumentoArrendador())
                .map(Usuario::nombreCompleto)
                .orElse(null));
        return dto;
    }
}
