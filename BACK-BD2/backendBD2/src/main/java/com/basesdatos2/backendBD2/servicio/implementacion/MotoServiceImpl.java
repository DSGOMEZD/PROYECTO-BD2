package com.basesdatos2.backendBD2.servicio.implementacion;

import com.basesdatos2.backendBD2.modelo.Motocicleta;
import com.basesdatos2.backendBD2.modelo.Seguro;
import com.basesdatos2.backendBD2.modelo.Usuario;
import com.basesdatos2.backendBD2.modelo.dto.MotocicletaRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.MotocicletaResponseDTO;
import com.basesdatos2.backendBD2.modelo.dto.SeguroRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.SeguroResponseDTO;
import com.basesdatos2.backendBD2.modelo.emun.EstadoMotocicleta;
import com.basesdatos2.backendBD2.modelo.emun.ModuloLog;
import com.basesdatos2.backendBD2.repositorio.MotoRepository;
import com.basesdatos2.backendBD2.repositorio.UsuarioRepository;
import com.basesdatos2.backendBD2.servicio.LogService;
import com.basesdatos2.backendBD2.servicio.MotoService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MotoServiceImpl implements MotoService {
    private final MotoRepository motoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LogService logService;
    private final ModelMapper mm;

    public MotoServiceImpl(MotoRepository motoRepository,
                           UsuarioRepository usuarioRepository,
                           LogService logService,
                           ModelMapper mm) {
        this.motoRepository = motoRepository;
        this.usuarioRepository = usuarioRepository;
        this.logService = logService;
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
        Integer idSeguro = motoRepository.registrar(aEntidad(documentoArrendador, dto), mm.map(dto.seguro(), Seguro.class));

        Map<String, Object> detalle = detalleMoto(dto);
        detalle.put("estado", motoRepository.buscar(dto.placa(), documentoArrendador)
                .map(Motocicleta::getEstado).orElse(null));
        detalle.put("idSeguro", idSeguro);

        logService.registrar(ModuloLog.MOTO, "PUBLICADA", documentoArrendador, dto.placa(),
                "Publicó la moto " + dto.marca() + " " + dto.modelo() + " (" + dto.placa() + ") a "
                        + LogService.pesos(dto.precioDia()) + "/día.",
                detalle);
        return idSeguro;
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
        EstadoMotocicleta anterior = motoRepository.buscar(placa, documentoArrendador)
                .map(Motocicleta::getEstado).orElse(null);

        motoRepository.cambiarEstado(placa, documentoArrendador, estado);

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("placa", placa);
        detalle.put("estadoAnterior", anterior);
        detalle.put("estadoNuevo", estado);
        logService.registrar(ModuloLog.MOTO, estado.name(), documentoArrendador, placa,
                "Cambió el estado de la moto " + placa + " de " + anterior + " a " + estado + ".",
                detalle);
    }

    @Override
    public void actualizar(
            String placa,
            Integer documentoArrendador,
            MotocicletaRequestDTO dto
    ) {
        Motocicleta antes = motoRepository.buscar(placa, documentoArrendador).orElse(null);

        Motocicleta moto = aEntidad(documentoArrendador, dto);
        moto.setPlaca(placa);
        motoRepository.actualizar(moto, mm.map(dto.seguro(), Seguro.class));

        Map<String, Object> detalle = detalleMoto(dto);
        detalle.put("placa", placa);
        if (antes != null) {
            detalle.put("precioDiaAnterior", antes.getPrecioAlquiler());
            detalle.put("kilometrajeAnterior", antes.getKilometrajeActual());
        }
        logService.registrar(ModuloLog.MOTO, "ACTUALIZADA", documentoArrendador, placa,
                "Actualizó la publicación de la moto " + dto.marca() + " " + dto.modelo() + " (" + placa + ").",
                detalle);
    }

    // Todo lo que el arrendador envio al publicar o editar la moto, con su seguro.
    private Map<String, Object> detalleMoto(MotocicletaRequestDTO dto) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("placa", dto.placa());
        detalle.put("marca", dto.marca());
        detalle.put("modelo", dto.modelo());
        detalle.put("anio", dto.anio());
        detalle.put("color", dto.color());
        detalle.put("cilindraje", dto.cilindraje());
        detalle.put("kilometraje", dto.kilometraje());
        detalle.put("precioDia", dto.precioDia());
        detalle.put("descripcion", dto.descripcion());

        SeguroRequestDTO seguro = dto.seguro();
        Map<String, Object> datosSeguro = new LinkedHashMap<>();
        datosSeguro.put("numeroPoliza", seguro.numeroPoliza());
        datosSeguro.put("aseguradora", seguro.aseguradora());
        datosSeguro.put("fechaInicioVigencia", seguro.fechaInicioVigencia());
        datosSeguro.put("estadoPoliza", seguro.estadoPoliza());
        detalle.put("seguro", datosSeguro);
        return detalle;
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
