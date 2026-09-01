package br.com.fiap.spacecrop.service;

import br.com.fiap.spacecrop.dto.request.FazendaRequestDTO;
import br.com.fiap.spacecrop.dto.response.FazendaResponseDTO;
import br.com.fiap.spacecrop.entity.Fazenda;
import br.com.fiap.spacecrop.entity.Usuario;
import br.com.fiap.spacecrop.exception.BusinessException;
import br.com.fiap.spacecrop.exception.ResourceNotFoundException;
import br.com.fiap.spacecrop.repository.FazendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FazendaService {

    private final FazendaRepository fazendaRepository;
    private final UsuarioService usuarioService;

    public Page<FazendaResponseDTO> listarPorUsuario(Long usuarioId, String nome, Pageable pageable) {
        return fazendaRepository.findByUsuarioIdWithFilters(usuarioId, nome, pageable)
                .map(this::toResponseDTO);
    }

    public Page<FazendaResponseDTO> listarTodas(String nome, String estado, Pageable pageable) {
        return fazendaRepository.findAllWithFilters(nome, estado, pageable)
                .map(this::toResponseDTO);
    }

    @Cacheable(value = "fazendas", key = "#id")
    public FazendaResponseDTO buscarPorId(Long id) {
        Fazenda fazenda = fazendaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fazenda não encontrada com ID: " + id));
        return toResponseDTO(fazenda);
    }

    @Transactional
    public FazendaResponseDTO criar(FazendaRequestDTO request, Long usuarioId) {
        Usuario usuario = usuarioService.buscarEntidadePorId(usuarioId);

        Fazenda fazenda = Fazenda.builder()
                .nome(request.getNome())
                .cidade(request.getCidade())
                .estado(request.getEstado())
                .areaHectares(request.getAreaHectares())
                .usuario(usuario)
                .build();

        fazenda = fazendaRepository.save(fazenda);
        return toResponseDTO(fazenda);
    }

    @Transactional
    @CacheEvict(value = "fazendas", key = "#id")
    public FazendaResponseDTO atualizar(Long id, FazendaRequestDTO request, Long usuarioId) {
        Fazenda fazenda = fazendaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fazenda não encontrada com ID: " + id));

        if (!fazenda.getUsuario().getId().equals(usuarioId)) {
            throw new BusinessException("Você não tem permissão para editar esta fazenda");
        }

        fazenda.setNome(request.getNome());
        fazenda.setCidade(request.getCidade());
        fazenda.setEstado(request.getEstado());
        fazenda.setAreaHectares(request.getAreaHectares());

        fazenda = fazendaRepository.save(fazenda);
        return toResponseDTO(fazenda);
    }

    @Transactional
    @CacheEvict(value = "fazendas", key = "#id")
    public void deletar(Long id, Long usuarioId) {
        Fazenda fazenda = fazendaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fazenda não encontrada com ID: " + id));

        if (!fazenda.getUsuario().getId().equals(usuarioId)) {
            throw new BusinessException("Você não tem permissão para deletar esta fazenda");
        }

        fazendaRepository.deleteById(id);
    }

    public Fazenda buscarEntidadePorId(Long id) {
        return fazendaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fazenda não encontrada com ID: " + id));
    }

    private FazendaResponseDTO toResponseDTO(Fazenda fazenda) {
        return FazendaResponseDTO.builder()
                .id(fazenda.getId())
                .nome(fazenda.getNome())
                .cidade(fazenda.getCidade())
                .estado(fazenda.getEstado())
                .areaHectares(fazenda.getAreaHectares())
                .usuarioId(fazenda.getUsuario().getId())
                .build();
    }
}