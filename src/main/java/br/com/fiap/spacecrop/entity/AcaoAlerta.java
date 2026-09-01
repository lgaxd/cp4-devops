package br.com.fiap.spacecrop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_ACAO_ALERTA")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class AcaoAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ACAO")
    @SequenceGenerator(name = "SEQ_ACAO", sequenceName = "SEQ_ACAO", allocationSize = 1)
    @Column(name = "id_acao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alerta", nullable = false)
    private Alerta alerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "ds_acao_tomada", nullable = false, length = 500)
    private String acaoTomada;

    @Column(name = "dt_acao", nullable = false)
    private LocalDateTime dataAcao;

    @PrePersist
    protected void onCreate() {
        dataAcao = LocalDateTime.now();
    }
}