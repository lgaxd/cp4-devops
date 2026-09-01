package br.com.fiap.spacecrop.service;

import br.com.fiap.spacecrop.dto.response.SensorOrbitalResponseDTO;
import br.com.fiap.spacecrop.entity.SensorOrbital;
import br.com.fiap.spacecrop.exception.ResourceNotFoundException;
import br.com.fiap.spacecrop.repository.SensorOrbitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SensorOrbitalService {

    private final SensorOrbitalRepository sensorOrbitalRepository;

    public Page<SensorOrbitalResponseDTO> listarPorSatelite(Long sateliteId, Pageable pageable) {
        return sensorOrbitalRepository.findBySateliteId(sateliteId, pageable)
                .map(this::toResponseDTO);
    }

    public Page<SensorOrbitalResponseDTO> listarTodos(String nome, String ativo, Pageable pageable) {
        return sensorOrbitalRepository.findAllWithFilters(nome, ativo, pageable)
                .map(this::toResponseDTO);
    }

    public SensorOrbital buscarEntidadePorId(Long id) {
        return sensorOrbitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor não encontrado com ID: " + id));
    }

    @Cacheable(value = "sensores", key = "#id")
    public SensorOrbitalResponseDTO buscarPorId(Long id) {
        SensorOrbital sensor = sensorOrbitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor não encontrado com ID: " + id));
        return toResponseDTO(sensor);
    }

    private SensorOrbitalResponseDTO toResponseDTO(SensorOrbital sensor) {
        return SensorOrbitalResponseDTO.builder()
                .id(sensor.getId())
                .nome(sensor.getNome())
                .ativo(sensor.getAtivo())
                .sateliteId(sensor.getSatelite().getId())
                .sateliteNome(sensor.getSatelite().getNome())
                .idTipoSensor(sensor.getIdTipoSensor())
                .build();
    }
}