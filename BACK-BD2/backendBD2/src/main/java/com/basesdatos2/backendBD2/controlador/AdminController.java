package com.basesdatos2.backendBD2.controlador;

import com.basesdatos2.backendBD2.modelo.dto.LogResponseDTO;
import com.basesdatos2.backendBD2.modelo.emun.ModuloLog;
import com.basesdatos2.backendBD2.servicio.LogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// Endpoints exclusivos del rol ADMIN.
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final LogService logService;

    public AdminController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping("/logs")
    public List<LogResponseDTO> listarLogs(
            @RequestParam Integer documentoAdmin,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) ModuloLog modulo
    ) {
        return logService.listar(documentoAdmin, desde, hasta, modulo);
    }
}
