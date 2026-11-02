package com.basesdatos2.backendBD2.servicio.implementacion;

import com.basesdatos2.backendBD2.modelo.Pago;
import com.basesdatos2.backendBD2.modelo.Usuario;
import com.basesdatos2.backendBD2.modelo.dto.PagoResponseDTO;
import com.basesdatos2.backendBD2.modelo.emun.MetodoPago;
import com.basesdatos2.backendBD2.modelo.emun.ModuloLog;
import com.basesdatos2.backendBD2.modelo.excepcion.RecursoNoEncontradoException;
import com.basesdatos2.backendBD2.repositorio.PagoRepository;
import com.basesdatos2.backendBD2.repositorio.UsuarioRepository;
import com.basesdatos2.backendBD2.servicio.LogService;
import com.basesdatos2.backendBD2.servicio.PagoService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PagoServiceImpl implements PagoService {
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LogService logService;
    private final ModelMapper mm;

    public PagoServiceImpl(PagoRepository pagoRepository,
                           UsuarioRepository usuarioRepository,
                           LogService logService,
                           ModelMapper mm) {
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.logService = logService;
        this.mm = mm;
    }

    @Override
    public List<PagoResponseDTO> listarMios(Integer documentoArrendatario) {
        return pagoRepository.listarMios(documentoArrendatario).stream()
                .map(p -> mm.map(p, PagoResponseDTO.class))
                .toList();
    }

    @Override
    public List<PagoResponseDTO> listarRecibidos(Integer documentoArrendador) {
        return pagoRepository.listarRecibidos(documentoArrendador).stream()
                .map(p -> mm.map(p, PagoResponseDTO.class))
                .toList();
    }

    @Override
    public void registrar(Integer idPago, Integer documentoArrendatario, MetodoPago metodoPago) {
        Pago antes = pagoRepository.buscarPorId(idPago)
                .orElseThrow(() -> new RecursoNoEncontradoException("El pago no existe."));

        pagoRepository.registrar(idPago, documentoArrendatario, metodoPago);

        Pago despues = pagoRepository.buscarPorId(idPago).orElse(antes);
        LocalDate hoy = LocalDate.now();

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("idPago", idPago);
        detalle.put("idContrato", antes.getIdContrato());
        detalle.put("monto", antes.getMonto());
        detalle.put("metodoPago", metodoPago);
        detalle.put("estadoAnterior", antes.getEstado());
        detalle.put("estadoNuevo", despues.getEstado());
        detalle.put("fechaPago", despues.getFechaPago());
        detalle.put("fechaVencimiento", antes.getFechaVencimiento());
        detalle.put("pagadoATiempo", !hoy.isAfter(antes.getFechaVencimiento()));
        detalle.put("diasRespectoAlVencimiento", ChronoUnit.DAYS.between(antes.getFechaVencimiento(), hoy));
        detalle.put("documentoArrendatario", documentoArrendatario);
        detalle.put("documentoArrendador", antes.getDocumentoArrendador());
        detalle.put("nombreArrendador", nombreDe(antes.getDocumentoArrendador()));

        logService.registrar(ModuloLog.PAGO, "PAGADO", documentoArrendatario, "Pago #" + idPago,
                "Pagó " + LogService.pesos(antes.getMonto()) + " del contrato #" + antes.getIdContrato()
                        + " por " + metodoPago.name() + ".",
                detalle);
    }

    private String nombreDe(Integer documento) {
        return usuarioRepository.buscarPorDocumento(documento)
                .map(Usuario::nombreCompleto)
                .orElse(null);
    }
}
