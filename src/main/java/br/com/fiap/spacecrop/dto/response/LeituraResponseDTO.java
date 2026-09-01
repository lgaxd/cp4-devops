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
public class LeituraResponseDTO {
    private Long id;
    private Double valor;
    private LocalDateTime dataLeitura;
    private String anomalia;
    private Long sensorOrbitalId;
    private String sensorNome;
    private Long fazendaId;
    private String fazendaNome;
    private Long setorId;
    private String setorNome;
}