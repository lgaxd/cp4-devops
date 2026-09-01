package br.com.fiap.spacecrop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "TB_SETOR_PLANTIO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class SetorPlantio {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SETOR")
    @SequenceGenerator(name = "SEQ_SETOR", sequenceName = "SEQ_SETOR", allocationSize = 1)
    @Column(name = "id_setor")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_fazenda", nullable = false)
    private Fazenda fazenda;

    @Column(name = "nm_setor", nullable = false, length = 100)
    private String nome;

    @Column(name = "ds_cultura", nullable = false, length = 100)
    private String cultura;

    @Column(name = "nr_area_hectares", nullable = false, columnDefinition = "NUMBER(10,2)")
    private Double areaHectares;
    
    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "setor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LeituraSatelite> leituras = new ArrayList<>();
}