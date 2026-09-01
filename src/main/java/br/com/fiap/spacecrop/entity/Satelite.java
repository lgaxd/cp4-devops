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
@Table(name = "TB_SATELITE")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Satelite {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SATELITE")
    @SequenceGenerator(name = "SEQ_SATELITE", sequenceName = "SEQ_SATELITE", allocationSize = 1)
    @Column(name = "id_satelite")
    private Long id;

    @Column(name = "nm_satelite", nullable = false, length = 100)
    private String nome;

    @Column(name = "ds_operador", nullable = false, length = 100)
    private String operador;

    @Column(name = "fl_ativo", nullable = false, columnDefinition = "CHAR(1)")
    private String ativo;

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "satelite", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SensorOrbital> sensores = new ArrayList<>();
}