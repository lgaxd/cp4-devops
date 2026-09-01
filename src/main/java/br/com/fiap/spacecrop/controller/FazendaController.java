package br.com.fiap.spacecrop.controller;

import br.com.fiap.spacecrop.dto.request.FazendaRequestDTO;
import br.com.fiap.spacecrop.dto.request.SetorPlantioRequestDTO;
import br.com.fiap.spacecrop.dto.response.AlertaResponseDTO;
import br.com.fiap.spacecrop.dto.response.FazendaResponseDTO;
import br.com.fiap.spacecrop.dto.response.LeituraResponseDTO;
import br.com.fiap.spacecrop.dto.response.SetorPlantioResponseDTO;
import br.com.fiap.spacecrop.entity.Usuario;
import br.com.fiap.spacecrop.service.AlertaService;
import br.com.fiap.spacecrop.service.FazendaService;
import br.com.fiap.spacecrop.service.LeituraService;
import br.com.fiap.spacecrop.service.SetorPlantioService;
import br.com.fiap.spacecrop.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/fazendas")
@RequiredArgsConstructor
@Tag(name = "Fazendas", description = "Endpoints para gerenciamento de fazendas")
@SecurityRequirement(name = "bearerAuth")
public class FazendaController {

    private final FazendaService fazendaService;
    private final SetorPlantioService setorPlantioService;
    private final LeituraService leituraService;
    private final AlertaService alertaService;
    private final UsuarioService usuarioService;

    private Long getUsuarioId(UserDetails userDetails) {
        Usuario usuario = usuarioService.buscarEntidadePorEmail(userDetails.getUsername());
        return usuario.getId();
    }

    @GetMapping
    @Operation(summary = "Listar fazendas do usuário", description = "Retorna todas as fazendas do usuário logado")
    public ResponseEntity<Page<FazendaResponseDTO>> listarFazendas(
            @RequestParam(required = false) String nome,
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {

        Long usuarioId = getUsuarioId(userDetails);
        return ResponseEntity.ok(fazendaService.listarPorUsuario(usuarioId, nome, pageable));
    }

    @GetMapping("/todas")
    @Operation(summary = "Listar todas as fazendas", description = "Retorna todas as fazendas do sistema (requer autenticação)")
    public ResponseEntity<Page<FazendaResponseDTO>> listarTodasFazendas(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String estado,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {

        return ResponseEntity.ok(fazendaService.listarTodas(nome, estado, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar fazenda por ID")
    public ResponseEntity<FazendaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(fazendaService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastrar fazenda")
    public ResponseEntity<FazendaResponseDTO> criar(
            @Valid @RequestBody FazendaRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long usuarioId = getUsuarioId(userDetails);
        FazendaResponseDTO fazenda = fazendaService.criar(request, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(fazenda);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar fazenda")
    public ResponseEntity<FazendaResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody FazendaRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long usuarioId = getUsuarioId(userDetails);
        return ResponseEntity.ok(fazendaService.atualizar(id, request, usuarioId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar fazenda")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long usuarioId = getUsuarioId(userDetails);
        fazendaService.deletar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{fazendaId}/setores")
    @Operation(summary = "Listar setores da fazenda")
    public ResponseEntity<Page<SetorPlantioResponseDTO>> listarSetores(
            @PathVariable Long fazendaId,
            @RequestParam(required = false) String cultura,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {

        return ResponseEntity.ok(setorPlantioService.listarPorFazenda(fazendaId, cultura, pageable));
    }

    @PostMapping("/{fazendaId}/setores")
    @Operation(summary = "Cadastrar setor na fazenda")
    public ResponseEntity<SetorPlantioResponseDTO> criarSetor(
            @PathVariable Long fazendaId,
            @Valid @RequestBody SetorPlantioRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long usuarioId = getUsuarioId(userDetails);
        SetorPlantioResponseDTO setor = setorPlantioService.criar(request, fazendaId, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(setor);
    }

    @GetMapping("/{fazendaId}/leituras")
    @Operation(summary = "Listar leituras da fazenda")
    public ResponseEntity<Page<LeituraResponseDTO>> listarLeituras(
            @PathVariable Long fazendaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
            @RequestParam(required = false) String anomalia,
            @PageableDefault(size = 10, sort = "dataLeitura") Pageable pageable) {

        return ResponseEntity.ok(leituraService.listarPorFazenda(fazendaId, dataInicio, dataFim, anomalia, pageable));
    }

    @GetMapping("/{fazendaId}/alertas")
    @Operation(summary = "Listar alertas da fazenda")
    public ResponseEntity<Page<AlertaResponseDTO>> listarAlertas(
            @PathVariable Long fazendaId,
            @PageableDefault(size = 10, sort = "dataAlerta") Pageable pageable) {

        return ResponseEntity.ok(alertaService.listarPorFazenda(fazendaId, pageable));
    }
}