package com.basesdatos2.backendBD2.servicio.implementacion;

import com.basesdatos2.backendBD2.modelo.Contrato;
import com.basesdatos2.backendBD2.modelo.Motocicleta;
import com.basesdatos2.backendBD2.modelo.Pago;
import com.basesdatos2.backendBD2.modelo.Usuario;
import com.basesdatos2.backendBD2.modelo.dto.ContratoResponseDTO;
import com.basesdatos2.backendBD2.modelo.dto.MotoResumenDTO;
import com.basesdatos2.backendBD2.modelo.emun.ModuloLog;
import com.basesdatos2.backendBD2.modelo.excepcion.RecursoNoEncontradoException;
import com.basesdatos2.backendBD2.repositorio.ContratoRepository;
import com.basesdatos2.backendBD2.repositorio.MotoRepository;
import com.basesdatos2.backendBD2.repositorio.PagoRepository;
import com.basesdatos2.backendBD2.repositorio.UsuarioRepository;
import com.basesdatos2.backendBD2.servicio.ContratoService;
import com.basesdatos2.backendBD2.servicio.LogService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContratoServiceImpl implements ContratoService {
    private final ContratoRepository contratoRepository;
    private final MotoRepository motoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PagoRepository pagoRepository;
    private final LogService logService;
    private final ModelMapper mm;

    public ContratoServiceImpl(ContratoRepository contratoRepository,
                               MotoRepository motoRepository,
                               UsuarioRepository usuarioRepository,
                               PagoRepository pagoRepository,
                               LogService logService,
                               ModelMapper mm) {
        this.contratoRepository = contratoRepository;
        this.motoRepository = motoRepository;
        this.usuarioRepository = usuarioRepository;
        this.pagoRepository = pagoRepository;
        this.logService = logService;
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
        ContratoResponseDTO antes = buscarPorId(idContrato);

        contratoRepository.cancelar(idContrato, documento);

        Map<String, Object> detalle = detalleContrato(antes);
        detalle.put("estadoAnterior", antes.getEstado());
        detalle.put("estadoNuevo", buscarPorId(idContrato).getEstado());
        detalle.put("canceladoPor", documento.equals(antes.getDocumentoArrendador()) ? "ARRENDADOR" : "ARRENDATARIO");

        logService.registrar(ModuloLog.CONTRATO, "CANCELADO", documento, "Contrato #" + idContrato,
                "Canceló el contrato #" + idContrato + " de la moto " + antes.getMoto().getPlaca() + ".",
                detalle);
    }

    @Override
    public void finalizar(
            Integer idContrato,
            Integer documentoArrendador,
            Integer kilometrajeFinal
    ) {
        ContratoResponseDTO antes = buscarPorId(idContrato);
        Integer kilometrajeInicial = motoRepository.buscar(antes.getMoto().getPlaca(), documentoArrendador)
                .map(Motocicleta::getKilometrajeActual)
                .orElse(null);

        contratoRepository.finalizar(
                idContrato,
                documentoArrendador,
                kilometrajeFinal
        );

        Map<String, Object> detalle = detalleContrato(antes);
        detalle.put("estadoAnterior", antes.getEstado());
        detalle.put("estadoNuevo", buscarPorId(idContrato).getEstado());
        detalle.put("kilometrajeInicial", kilometrajeInicial);
        detalle.put("kilometrajeFinal", kilometrajeFinal);
        if (kilometrajeInicial != null) {
            detalle.put("kilometrosRecorridos", kilometrajeFinal - kilometrajeInicial);
        }

        logService.registrar(ModuloLog.CONTRATO, "FINALIZADO", documentoArrendador, "Contrato #" + idContrato,
                "Finalizó el contrato #" + idContrato + " de la moto " + antes.getMoto().getPlaca()
                        + " (km final " + kilometrajeFinal + ").",
                detalle);
    }

    @Override
    public void registrarLogGenerado(ContratoResponseDTO contrato, Integer documentoArrendador) {
        Map<String, Object> detalle = detalleContrato(contrato);
        detalle.put("pagosGenerados", pagoRepository.listarPorContrato(contrato.getIdContrato()).stream()
                .map(this::resumenPago)
                .toList());

        logService.registrar(ModuloLog.CONTRATO, "GENERADO", documentoArrendador,
                "Contrato #" + contrato.getIdContrato(),
                "Se generó el contrato #" + contrato.getIdContrato() + " para la moto "
                        + contrato.getMoto().getPlaca() + " con " + contrato.getNombreArrendatario()
                        + " por " + LogService.pesos(contrato.getValorTotal()) + ".",
                detalle);
    }

    // Snapshot completo del contrato: fechas, valores, partes y moto.
    private Map<String, Object> detalleContrato(ContratoResponseDTO c) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("idContrato", c.getIdContrato());
        detalle.put("idSolicitud", c.getIdSolicitud());
        detalle.put("estado", c.getEstado());
        detalle.put("fechaInicio", c.getFechaInicio());
        detalle.put("fechaFin", c.getFechaFin());
        detalle.put("dias", ChronoUnit.DAYS.between(c.getFechaInicio(), c.getFechaFin()) + 1);
        detalle.put("valorTotal", c.getValorTotal());
        detalle.put("deposito", c.getDeposito());
        detalle.put("documentoArrendatario", c.getDocumentoArrendatario());
        detalle.put("nombreArrendatario", c.getNombreArrendatario());
        detalle.put("documentoArrendador", c.getDocumentoArrendador());
        detalle.put("nombreArrendador", c.getNombreArrendador());

        MotoResumenDTO moto = c.getMoto();
        Map<String, Object> datosMoto = new LinkedHashMap<>();
        datosMoto.put("placa", moto.getPlaca());
        datosMoto.put("marca", moto.getMarca());
        datosMoto.put("modelo", moto.getModelo());
        datosMoto.put("precioDia", moto.getPrecioDia());
        detalle.put("moto", datosMoto);
        return detalle;
    }

    private Map<String, Object> resumenPago(Pago pago) {
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("idPago", pago.getIdPago());
        resumen.put("monto", pago.getMonto());
        resumen.put("fechaVencimiento", pago.getFechaVencimiento());
        resumen.put("estado", pago.getEstado());
        return resumen;
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
