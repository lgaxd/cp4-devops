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
@Table(name = "TB_FAZENDA")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Fazenda {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FAZENDA")
    @SequenceGenerator(name = "SEQ_FAZENDA", sequenceName = "SEQ_FAZENDA", allocationSize = 1)
    @Column(name = "id_fazenda")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "nm_fazenda", nullable = false, length = 150)
    private String nome;

    @Column(name = "ds_cidade", nullable = false, length = 100)
    private String cidade;

    @Column(name = "ds_estado", columnDefinition = "CHAR(2)")
    private String estado;

    @Column(name = "nr_area_hectares", nullable = false, columnDefinition = "NUMBER(10,2)")
    private Double areaHectares;

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "fazenda", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SetorPlantio> setores = new ArrayList<>();

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "fazenda", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LeituraSatelite> leituras = new ArrayList<>();

}