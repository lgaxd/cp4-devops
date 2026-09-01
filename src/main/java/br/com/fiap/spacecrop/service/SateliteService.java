package br.com.fiap.spacecrop.service;

import br.com.fiap.spacecrop.dto.response.SateliteResponseDTO;
import br.com.fiap.spacecrop.entity.Satelite;
import br.com.fiap.spacecrop.exception.ResourceNotFoundException;
import br.com.fiap.spacecrop.repository.SateliteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SateliteService {

    private final SateliteRepository sateliteRepository;

    public Page<SateliteResponseDTO> listarTodos(String nome, String operador, Pageable pageable) {
        return sateliteRepository.findAllWithFilters(nome, operador, pageable)
            .map(this::toResponseDTO);
    }

    @Cacheable(value = "satelites", key = "#id")
    public SateliteResponseDTO buscarPorId(Long id) {
        Satelite satelite = sateliteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Satélite não encontrado com ID: " + id));
        return toResponseDTO(satelite);
    }

    private SateliteResponseDTO toResponseDTO(Satelite satelite) {
        return SateliteResponseDTO.builder()
            .id(satelite.getId())
            .nome(satelite.getNome())
            .operador(satelite.getOperador())
            .ativo(satelite.getAtivo())
            .build();
    }
}