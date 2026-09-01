package br.com.fiap.spacecrop.controller;

import br.com.fiap.spacecrop.dto.request.LeituraRequestDTO;
import br.com.fiap.spacecrop.dto.response.LeituraResponseDTO;
import br.com.fiap.spacecrop.entity.Usuario;
import br.com.fiap.spacecrop.service.LeituraService;
import br.com.fiap.spacecrop.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/leituras")
@RequiredArgsConstructor
@Tag(name = "Leituras", description = "Endpoints para gerenciamento de leituras")
@SecurityRequirement(name = "bearerAuth")
public class LeituraController {

    private final LeituraService leituraService;
    private final UsuarioService usuarioService;

    private Long getUsuarioId(UserDetails userDetails) {
        Usuario usuario = usuarioService.buscarEntidadePorEmail(userDetails.getUsername());
        return usuario.getId();
    }

    @PostMapping
    @Operation(summary = "Inserir nova leitura", description = "Simula uma leitura de satélite")
    public ResponseEntity<LeituraResponseDTO> criarLeitura(
            @Valid @RequestBody LeituraRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long usuarioId = getUsuarioId(userDetails);
        LeituraResponseDTO leitura = leituraService.criar(request, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(leitura);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar leitura por ID")
    public ResponseEntity<LeituraResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(leituraService.buscarPorId(id));
    }
}