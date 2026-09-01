package br.com.fiap.spacecrop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SateliteResponseDTO {
    private Long id;
    private String nome;
    private String operador;
    private String ativo;
}