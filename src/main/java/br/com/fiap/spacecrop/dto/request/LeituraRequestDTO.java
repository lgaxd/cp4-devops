package br.com.fiap.spacecrop.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeituraRequestDTO {

    @NotNull(message = "ID do sensor orbital é obrigatório")
    private Long sensorOrbitalId;

    @NotNull(message = "ID da fazenda é obrigatório")
    private Long fazendaId;

    private Long setorId;

    @NotNull(message = "Valor da leitura é obrigatório")
    private Double valor;
}