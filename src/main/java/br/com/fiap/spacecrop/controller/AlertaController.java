package br.com.fiap.spacecrop.controller;

import br.com.fiap.spacecrop.dto.request.ResolverAlertaRequestDTO;
import br.com.fiap.spacecrop.dto.response.AlertaResponseDTO;
import br.com.fiap.spacecrop.entity.Usuario;
import br.com.fiap.spacecrop.service.AlertaService;
import br.com.fiap.spacecrop.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/alertas")
@RequiredArgsConstructor
@Tag(name = "Alertas", description = "Endpoints para gerenciamento de alertas")
@SecurityRequirement(name = "bearerAuth")
public class AlertaController {

    private final AlertaService alertaService;
    private final UsuarioService usuarioService;

    private Long getUsuarioId(UserDetails userDetails) {
        Usuario usuario = usuarioService.buscarEntidadePorEmail(userDetails.getUsername());
        return usuario.getId();
    }

    @GetMapping
    @Operation(summary = "Listar alertas com busca", description = "Retorna alertas com filtros por resolução e tipo")
    public ResponseEntity<Page<AlertaResponseDTO>> listarAlertas(
            @RequestParam(required = false) String resolvido,
            @RequestParam(required = false) Long idTipoAlerta,
            @RequestParam(required = false) Long fazendaId,
            @PageableDefault(size = 10, sort = "dataAlerta") Pageable pageable) {
        
        return ResponseEntity.ok(alertaService.listarTodos(resolvido != null ? resolvido.charAt(0) : null, idTipoAlerta, fazendaId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar alerta por ID")
    public ResponseEntity<AlertaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(alertaService.buscarPorId(id));
    }

    @PutMapping("/{id}/resolver")
    @Operation(summary = "Resolver alerta", description = "Marca o alerta como resolvido e registra a ação")
    public ResponseEntity<AlertaResponseDTO> resolverAlerta(
            @PathVariable Long id,
            @Valid @RequestBody ResolverAlertaRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long usuarioId = getUsuarioId(userDetails);
        AlertaResponseDTO alerta = alertaService.resolver(id, request, usuarioId);
        return ResponseEntity.ok(alerta);
    }
}