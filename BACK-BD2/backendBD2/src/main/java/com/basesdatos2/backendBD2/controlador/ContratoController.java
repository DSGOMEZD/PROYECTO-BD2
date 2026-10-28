package com.basesdatos2.backendBD2.controlador;

import com.basesdatos2.backendBD2.modelo.dto.ContratoResponseDTO;
import com.basesdatos2.backendBD2.servicio.ContratoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.basesdatos2.backendBD2.modelo.dto.FinalizarContratoRequestDTO;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/contratos")
public class ContratoController {
    private final ContratoService contratoService;

    public ContratoController(ContratoService contratoService) {
        this.contratoService = contratoService;
    }

    @GetMapping("/mios")
    public List<ContratoResponseDTO> listarMios(
            @RequestParam Integer documentoArrendatario
    ) {
        return contratoService.listarMios(documentoArrendatario);
    }

    @GetMapping("/arrendador")
    public List<ContratoResponseDTO> listarComoArrendador(
            @RequestParam Integer documentoArrendador
    ) {
        return contratoService.listarComoArrendador(documentoArrendador);
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(
            @PathVariable Integer id,
            @RequestParam Integer documento
    ) {
        contratoService.cancelar(id, documento);
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/{id}/finalizar")
public ResponseEntity<Void> finalizar(
        @PathVariable Integer id,
        @RequestParam Integer documentoArrendador,
        @Valid @RequestBody FinalizarContratoRequestDTO dto
) {
    contratoService.finalizar(
            id,
            documentoArrendador,
            dto.kilometrajeFinal()
    );
    return ResponseEntity.noContent().build();
}
}