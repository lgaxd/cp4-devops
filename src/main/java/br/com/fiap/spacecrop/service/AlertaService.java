package br.com.fiap.spacecrop.service;

import br.com.fiap.spacecrop.dto.request.ResolverAlertaRequestDTO;
import br.com.fiap.spacecrop.dto.response.AlertaResponseDTO;
import br.com.fiap.spacecrop.entity.AcaoAlerta;
import br.com.fiap.spacecrop.entity.Alerta;
import br.com.fiap.spacecrop.entity.Usuario;
import br.com.fiap.spacecrop.exception.BusinessException;
import br.com.fiap.spacecrop.exception.ResourceNotFoundException;
import br.com.fiap.spacecrop.repository.AcaoAlertaRepository;
import br.com.fiap.spacecrop.repository.AlertaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertaService {

    private final AlertaRepository alertaRepository;
    private final AcaoAlertaRepository acaoAlertaRepository;
    private final UsuarioService usuarioService;

    public Page<AlertaResponseDTO> listarPorFazenda(Long fazendaId, Pageable pageable) {
        return alertaRepository.findByLeituraFazendaId(fazendaId, pageable)
            .map(this::toResponseDTO);
    }

    public Page<AlertaResponseDTO> listarTodos(Character resolvido, Long idTipoAlerta, Long fazendaId, Pageable pageable) {
        return alertaRepository.findAllWithFilters(resolvido, idTipoAlerta, fazendaId, pageable)
            .map(this::toResponseDTO);
    }

    public AlertaResponseDTO buscarPorId(Long id) {
        Alerta alerta = alertaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Alerta não encontrado com ID: " + id));
        return toResponseDTO(alerta);
    }

    @Transactional
    public AlertaResponseDTO resolver(Long id, ResolverAlertaRequestDTO request, Long usuarioId) {
        Alerta alerta = alertaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Alerta não encontrado com ID: " + id));

        if ((char) 'S' == alerta.getResolvido()) {
            throw new BusinessException("Este alerta já foi resolvido");
        }

        Usuario usuario = usuarioService.buscarEntidadePorId(usuarioId);

        alerta.setResolvido((char) 'S');

        AcaoAlerta acao = new AcaoAlerta();
        acao.setAlerta(alerta);
        acao.setUsuario(usuario);
        acao.setAcaoTomada(request.getAcaoTomada());

        alerta = alertaRepository.save(alerta);
        acaoAlertaRepository.save(acao);

        return toResponseDTO(alerta);
    }

    private AlertaResponseDTO toResponseDTO(Alerta alerta) {
        return AlertaResponseDTO.builder()
            .id(alerta.getId())
            .idTipoAlerta(alerta.getIdTipoAlerta())
            .resolvido(alerta.getResolvido().toString())
            .dataAlerta(alerta.getDataAlerta())
            .leituraId(alerta.getLeitura().getId())
            .valorLeitura(alerta.getLeitura().getValor())
            .fazendaId(alerta.getLeitura().getFazenda().getId())
            .fazendaNome(alerta.getLeitura().getFazenda().getNome())
            .usuarioId(alerta.getUsuario().getId())
            .build();
    }
}