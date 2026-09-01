package br.com.fiap.spacecrop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorOrbitalResponseDTO {
    private Long id;
    private String nome;
    private String ativo;
    private Long sateliteId;
    private String sateliteNome;
    private Long idTipoSensor;
}