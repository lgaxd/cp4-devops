package br.com.fiap.spacecrop.controller;

import br.com.fiap.spacecrop.dto.response.SensorOrbitalResponseDTO;
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
@RequestMapping("/sensores")
@RequiredArgsConstructor
@Tag(name = "Sensores Orbitais", description = "Endpoints para consulta de sensores orbitais")
@SecurityRequirement(name = "bearerAuth")
public class SensorOrbitalController {

    private final SensorOrbitalService sensorOrbitalService;

    @GetMapping
    @Operation(summary = "Listar sensores", description = "Retorna lista paginada de sensores orbitais")
    @Cacheable(value = "sensores_list", key = "{#nome, #ativo, #pageable.pageNumber, #pageable.pageSize}")
    public ResponseEntity<Page<SensorOrbitalResponseDTO>> listarSensores(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String ativo,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        
        return ResponseEntity.ok(sensorOrbitalService.listarTodos(nome, ativo, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar sensor por ID")
    public ResponseEntity<SensorOrbitalResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(sensorOrbitalService.buscarPorId(id));
    }
}