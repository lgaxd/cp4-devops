package br.com.fiap.spacecrop.controller;

import br.com.fiap.spacecrop.dto.request.UsuarioUpdateRequestDTO;
import br.com.fiap.spacecrop.dto.response.UsuarioResponseDTO;
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
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Endpoints para gerenciamento de usuários")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @Operation(summary = "Listar usuários", description = "Retorna lista paginada de usuários")
    public ResponseEntity<Page<UsuarioResponseDTO>> listarUsuarios(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String email,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(usuarioService.listarTodos(nome, email, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID", description = "Retorna um usuário específico")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UsuarioResponseDTO usuario = usuarioService.buscarPorId(id);
        
        return ResponseEntity.ok(usuario);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar usuário", description = "Atualiza os dados de um usuário")
    public ResponseEntity<UsuarioResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        UsuarioResponseDTO usuario = usuarioService.buscarPorId(id);
        
        if (!usuario.getEmail().equals(userDetails.getUsername())) {
            throw new org.springframework.security.access.AccessDeniedException("Acesso negado");
        }
        
        return ResponseEntity.ok(usuarioService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar usuário", description = "Remove um usuário do sistema")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        usuarioService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}