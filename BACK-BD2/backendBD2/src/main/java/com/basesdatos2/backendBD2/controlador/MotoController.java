package com.basesdatos2.backendBD2.controlador;
import java.time.LocalDate;
import java.util.List;

import com.basesdatos2.backendBD2.modelo.dto.CambiarEstadoRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.MotocicletaRequestDTO;
import com.basesdatos2.backendBD2.modelo.dto.MotocicletaResponseDTO;
import com.basesdatos2.backendBD2.servicio.MotoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/motos")
public class MotoController {
    private final MotoService motoService;

    public MotoController(MotoService motoService) {
        this.motoService = motoService;
    }

    @GetMapping("/disponibles")
    public List<MotocicletaResponseDTO> listarDisponibles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin
    ) {
        return motoService.listarDisponibles(fechaInicio, fechaFin);
    }

    @GetMapping("/mias")
    public ResponseEntity<List<MotocicletaResponseDTO>> listarMias(
            @RequestParam Integer documentoArrendador) {
        return ResponseEntity.ok(motoService.listarMias(documentoArrendador));
    }

    @PostMapping
    public ResponseEntity<Integer> registrar(
            @RequestParam Integer documentoArrendador,
            @Valid @RequestBody MotocicletaRequestDTO dto
    ) {
        Integer idSeguro = motoService.registrar(documentoArrendador, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(idSeguro);
    }

    @PatchMapping("/{placa}/estado")
    public ResponseEntity<Void> cambiarEstado(
            @PathVariable String placa,
            @RequestParam Integer documentoArrendador,
            @Valid @RequestBody CambiarEstadoRequestDTO request) {

        motoService.cambiarEstado(
                placa,
                documentoArrendador,
                request.estado()
        );

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{placa}")
    public ResponseEntity<Void> actualizar(
            @PathVariable String placa,
            @RequestParam Integer documentoArrendador,
            @Valid @RequestBody MotocicletaRequestDTO dto
    ) {
        motoService.actualizar(placa, documentoArrendador, dto);
        return ResponseEntity.noContent().build();
    }
}
