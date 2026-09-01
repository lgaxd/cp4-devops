package br.com.fiap.spacecrop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetorPlantioRequestDTO {

    @NotBlank(message = "Nome do setor é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @NotBlank(message = "Cultura é obrigatória")
    @Size(max = 100, message = "Cultura deve ter no máximo 100 caracteres")
    private String cultura;

    @NotNull(message = "Área em hectares é obrigatória")
    @Positive(message = "Área deve ser maior que zero")
    private Double areaHectares;
}