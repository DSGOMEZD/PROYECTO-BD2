package com.basesdatos2.backendBD2.controlador;

import com.basesdatos2.backendBD2.modelo.dto.PagoResponseDTO;
import com.basesdatos2.backendBD2.modelo.dto.RegistrarPagoRequestDTO;
import com.basesdatos2.backendBD2.servicio.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {
    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @GetMapping("/mios")
    public List<PagoResponseDTO> listarMios(
            @RequestParam Integer documentoArrendatario
    ) {
        return pagoService.listarMios(documentoArrendatario);
    }

    @GetMapping("/recibidos")
    public List<PagoResponseDTO> listarRecibidos(
            @RequestParam Integer documentoArrendador
    ) {
        return pagoService.listarRecibidos(documentoArrendador);
    }

    @PatchMapping("/{id}/pagar")
    public ResponseEntity<Void> registrar(
            @PathVariable Integer id,
            @RequestParam Integer documentoArrendatario,
            @Valid @RequestBody RegistrarPagoRequestDTO dto
    ) {
        pagoService.registrar(id, documentoArrendatario, dto.metodoPago());
        return ResponseEntity.noContent().build();
    }
}