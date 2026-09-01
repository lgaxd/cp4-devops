package br.com.fiap.spacecrop.service;

import br.com.fiap.spacecrop.dto.request.LeituraRequestDTO;
import br.com.fiap.spacecrop.dto.response.LeituraResponseDTO;
import br.com.fiap.spacecrop.entity.Fazenda;
import br.com.fiap.spacecrop.entity.LeituraSatelite;
import br.com.fiap.spacecrop.entity.SensorOrbital;
import br.com.fiap.spacecrop.entity.SetorPlantio;
import br.com.fiap.spacecrop.exception.BusinessException;
import br.com.fiap.spacecrop.exception.ResourceNotFoundException;
import br.com.fiap.spacecrop.repository.LeituraSateliteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LeituraService {

    private final LeituraSateliteRepository leituraRepository;
    private final SensorOrbitalService sensorOrbitalService;
    private final FazendaService fazendaService;
    private final SetorPlantioService setorPlantioService;

    public Page<LeituraResponseDTO> listarPorFazenda(Long fazendaId, LocalDateTime dataInicio,
                                                      LocalDateTime dataFim, String anomalia, Pageable pageable) {
        return leituraRepository.findByFazendaIdWithFilters(fazendaId, dataInicio, dataFim, anomalia, pageable)
            .map(this::toResponseDTO);
    }

    public LeituraResponseDTO buscarPorId(Long id) {
        LeituraSatelite leitura = leituraRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Leitura não encontrada com ID: " + id));
        return toResponseDTO(leitura);
    }

    public LeituraSatelite buscarEntidadePorId(Long id) {
        return leituraRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Leitura não encontrada com ID: " + id));
    }

    @Transactional
    public LeituraResponseDTO criar(LeituraRequestDTO request, Long usuarioId) {
        SensorOrbital sensor = sensorOrbitalService.buscarEntidadePorId(request.getSensorOrbitalId());
        Fazenda fazenda = fazendaService.buscarEntidadePorId(request.getFazendaId());

        if (!fazenda.getUsuario().getId().equals(usuarioId)) {
            throw new BusinessException("Você não tem permissão para adicionar leituras a esta fazenda");
        }

        SetorPlantio setor = null;
        if (request.getSetorId() != null) {
            setor = setorPlantioService.buscarEntidadePorId(request.getSetorId());
            if (!setor.getFazenda().getId().equals(fazenda.getId())) {
                throw new BusinessException("Setor não pertence à fazenda informada");
            }
        }

        LeituraSatelite leitura = new LeituraSatelite();
        leitura.setSensorOrbital(sensor);
        leitura.setFazenda(fazenda);
        leitura.setSetor(setor);
        leitura.setValor(request.getValor());
        leitura.setAnomalia("N");
        leitura.setDataLeitura(LocalDateTime.now());

        leitura = leituraRepository.save(leitura);
        return toResponseDTO(leitura);
    }

    private LeituraResponseDTO toResponseDTO(LeituraSatelite leitura) {
        return LeituraResponseDTO.builder()
            .id(leitura.getId())
            .valor(leitura.getValor())
            .dataLeitura(leitura.getDataLeitura())
            .anomalia(leitura.getAnomalia())
            .sensorOrbitalId(leitura.getSensorOrbital().getId())
            .sensorNome(leitura.getSensorOrbital().getNome())
            .fazendaId(leitura.getFazenda().getId())
            .fazendaNome(leitura.getFazenda().getNome())
            .setorId(leitura.getSetor() != null ? leitura.getSetor().getId() : null)
            .setorNome(leitura.getSetor() != null ? leitura.getSetor().getNome() : null)
            .build();
    }
}