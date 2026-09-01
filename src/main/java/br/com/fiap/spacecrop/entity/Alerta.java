package br.com.fiap.spacecrop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "TB_ALERTA")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ALERTA")
    @SequenceGenerator(name = "SEQ_ALERTA", sequenceName = "SEQ_ALERTA", allocationSize = 1)
    @Column(name = "id_alerta")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_leitura", nullable = false)
    private LeituraSatelite leitura;

    @Column(name = "id_tipo_alerta", nullable = false)
    private Long idTipoAlerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fl_resolvido", nullable = false, columnDefinition = "CHAR(1)")
    private Character resolvido;

    @Column(name = "dt_alerta", nullable = false)
    private LocalDateTime dataAlerta;

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "alerta", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AcaoAlerta> acoes = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        dataAlerta = LocalDateTime.now();
    }

}