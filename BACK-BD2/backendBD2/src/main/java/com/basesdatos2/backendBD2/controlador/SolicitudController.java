package com.basesdatos2.backendBD2.controlador;

import com.basesdatos2.backendBD2.modelo.dto.AprobarSolicitudRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.ContratoResponseDTO;
import com.basesdatos2.backendBD2.modelo.dto.SolicitudRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.SolicitudResponseDTO;
import com.basesdatos2.backendBD2.servicio.SolicitudService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @PostMapping
    public ResponseEntity<SolicitudResponseDTO> crear(
            @RequestParam Integer documentoArrendatario,
            @Valid @RequestBody SolicitudRequestDTO dto
    ) {
        SolicitudResponseDTO creada =
                solicitudService.crear(documentoArrendatario, dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/mias")
    public List<SolicitudResponseDTO> listarMias(
            @RequestParam Integer documentoArrendatario
    ) {
        return solicitudService.listarMias(documentoArrendatario);
    }

    @GetMapping("/recibidas")
    public List<SolicitudResponseDTO> listarRecibidas(
            @RequestParam Integer documentoArrendador
    ) {
        return solicitudService.listarRecibidas(documentoArrendador);
    }

    @PostMapping("/{id}/aprobar")
    public ResponseEntity<ContratoResponseDTO> aprobar(
            @PathVariable Integer id,
            @RequestParam Integer documentoArrendador,
            @Valid @RequestBody AprobarSolicitudRequestDTO dto
    ) {
        return ResponseEntity.ok(
                solicitudService.aprobar(id, documentoArrendador, dto.deposito())
        );
    }

    @PatchMapping("/{id}/rechazar")
    public ResponseEntity<Void> rechazar(
            @PathVariable Integer id,
            @RequestParam Integer documentoArrendador
    ) {
        solicitudService.rechazar(id, documentoArrendador);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(
            @PathVariable Integer id,
            @RequestParam Integer documentoArrendatario
    ) {
        solicitudService.cancelar(id, documentoArrendatario);
        return ResponseEntity.noContent().build();
    }
}
