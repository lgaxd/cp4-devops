package br.com.fiap.spacecrop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolverAlertaRequestDTO {

    @NotBlank(message = "Ação tomada é obrigatória")
    @Size(max = 500, message = "Ação deve ter no máximo 500 caracteres")
    private String acaoTomada;
}