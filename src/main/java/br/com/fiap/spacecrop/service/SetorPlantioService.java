package br.com.fiap.spacecrop.service;

import br.com.fiap.spacecrop.dto.request.SetorPlantioRequestDTO;
import br.com.fiap.spacecrop.dto.response.SetorPlantioResponseDTO;
import br.com.fiap.spacecrop.entity.Fazenda;
import br.com.fiap.spacecrop.entity.SetorPlantio;
import br.com.fiap.spacecrop.exception.BusinessException;
import br.com.fiap.spacecrop.exception.ResourceNotFoundException;
import br.com.fiap.spacecrop.repository.SetorPlantioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SetorPlantioService {

    private final SetorPlantioRepository setorPlantioRepository;
    private final FazendaService fazendaService;

    public Page<SetorPlantioResponseDTO> listarPorFazenda(Long fazendaId, String cultura, Pageable pageable) {
        return setorPlantioRepository.findByFazendaIdWithFilters(fazendaId, cultura, pageable)
                .map(this::toResponseDTO);
    }

    public SetorPlantioResponseDTO buscarPorId(Long id, Long fazendaId) {
        SetorPlantio setor = setorPlantioRepository.findByIdAndFazendaId(id, fazendaId)
                .orElseThrow(() -> new ResourceNotFoundException("Setor não encontrado"));
        return toResponseDTO(setor);
    }

    public SetorPlantio buscarEntidadePorId(Long id) {
        return setorPlantioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Setor não encontrado com ID: " + id));
    }

    @Transactional
    public SetorPlantioResponseDTO criar(SetorPlantioRequestDTO request, Long fazendaId, Long usuarioId) {
        Fazenda fazenda = fazendaService.buscarEntidadePorId(fazendaId);

        if (!fazenda.getUsuario().getId().equals(usuarioId)) {
            throw new BusinessException("Você não tem permissão para adicionar setores a esta fazenda");
        }

        SetorPlantio setor = SetorPlantio.builder()
                .nome(request.getNome())
                .cultura(request.getCultura())
                .areaHectares(request.getAreaHectares())
                .fazenda(fazenda)
                .build();

        setor = setorPlantioRepository.save(setor);
        return toResponseDTO(setor);
    }

    @Transactional
    public SetorPlantioResponseDTO atualizar(Long id, SetorPlantioRequestDTO request, Long usuarioId) {
        SetorPlantio setor = setorPlantioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Setor não encontrado com ID: " + id));

        if (!setor.getFazenda().getUsuario().getId().equals(usuarioId)) {
            throw new BusinessException("Você não tem permissão para editar este setor");
        }

        setor.setNome(request.getNome());
        setor.setCultura(request.getCultura());
        setor.setAreaHectares(request.getAreaHectares());

        setor = setorPlantioRepository.save(setor);
        return toResponseDTO(setor);
    }

    @Transactional
    public void deletar(Long id, Long usuarioId) {
        SetorPlantio setor = setorPlantioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Setor não encontrado com ID: " + id));

        if (!setor.getFazenda().getUsuario().getId().equals(usuarioId)) {
            throw new BusinessException("Você não tem permissão para deletar este setor");
        }

        setorPlantioRepository.deleteById(id);
    }

    private SetorPlantioResponseDTO toResponseDTO(SetorPlantio setor) {
        return SetorPlantioResponseDTO.builder()
                .id(setor.getId())
                .nome(setor.getNome())
                .cultura(setor.getCultura())
                .areaHectares(setor.getAreaHectares())
                .fazendaId(setor.getFazenda().getId())
                .build();
    }
}