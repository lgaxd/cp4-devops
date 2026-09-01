package br.com.fiap.spacecrop.controller;

import br.com.fiap.spacecrop.dto.request.SetorPlantioRequestDTO;
import br.com.fiap.spacecrop.dto.response.SetorPlantioResponseDTO;
import br.com.fiap.spacecrop.entity.Usuario;
import br.com.fiap.spacecrop.service.SetorPlantioService;
import br.com.fiap.spacecrop.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/setores")
@RequiredArgsConstructor
@Tag(name = "Setores de Plantio", description = "Endpoints para gerenciamento de setores de plantio")
@SecurityRequirement(name = "bearerAuth")
public class SetorPlantioController {

    private final SetorPlantioService setorPlantioService;
    private final UsuarioService usuarioService;

    private Long getUsuarioId(UserDetails userDetails) {
        Usuario usuario = usuarioService.buscarEntidadePorEmail(userDetails.getUsername());
        return usuario.getId();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar setor")
    public ResponseEntity<SetorPlantioResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody SetorPlantioRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long usuarioId = getUsuarioId(userDetails);
        SetorPlantioResponseDTO setor = setorPlantioService.atualizar(id, request, usuarioId);
        return ResponseEntity.ok(setor);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar setor")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long usuarioId = getUsuarioId(userDetails);
        setorPlantioService.deletar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}