package br.com.fiap.spacecrop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertaResponseDTO {
    private Long id;
    private Long idTipoAlerta;
    private String resolvido;
    private LocalDateTime dataAlerta;
    private Long leituraId;
    private Double valorLeitura;
    private Long fazendaId;
    private String fazendaNome;
    private Long usuarioId;
}