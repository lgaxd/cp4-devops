package br.com.fiap.spacecrop.controller;

import br.com.fiap.spacecrop.dto.response.SateliteResponseDTO;
import br.com.fiap.spacecrop.dto.response.SensorOrbitalResponseDTO;
import br.com.fiap.spacecrop.service.SateliteService;
import br.com.fiap.spacecrop.service.SensorOrbitalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/satelites")
@RequiredArgsConstructor
@Tag(name = "Satélites", description = "Endpoints para consulta de satélites")
@SecurityRequirement(name = "bearerAuth")
public class SateliteController {

    private final SateliteService sateliteService;
    private final SensorOrbitalService sensorOrbitalService;

    @GetMapping
    @Operation(summary = "Listar satélites", description = "Retorna lista paginada de satélites")
    @Cacheable(value = "satelites_list", key = "{#nome, #operador, #pageable.pageNumber, #pageable.pageSize}")
    public ResponseEntity<Page<SateliteResponseDTO>> listarSatelites(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String operador,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        
        return ResponseEntity.ok(sateliteService.listarTodos(nome, operador, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar satélite por ID")
    public ResponseEntity<SateliteResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(sateliteService.buscarPorId(id));
    }

    @GetMapping("/{id}/sensores")
    @Operation(summary = "Listar sensores do satélite")
    public ResponseEntity<Page<SensorOrbitalResponseDTO>> listarSensoresPorSatelite(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        
        return ResponseEntity.ok(sensorOrbitalService.listarPorSatelite(id, pageable));
    }
}