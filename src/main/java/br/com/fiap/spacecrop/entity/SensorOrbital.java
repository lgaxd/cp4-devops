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
@Table(name = "TB_SENSOR_ORBITAL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class SensorOrbital {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SENSOR_ORBITAL")
    @SequenceGenerator(name = "SEQ_SENSOR_ORBITAL", sequenceName = "SEQ_SENSOR_ORBITAL", allocationSize = 1)
    @Column(name = "id_sensor_orbital")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_satelite", nullable = false)
    private Satelite satelite;

    @Column(name = "id_tipo_sensor", nullable = false)
    private Long idTipoSensor;

    @Column(name = "nm_sensor", nullable = false, length = 100)
    private String nome;

    @Column(name = "fl_ativo", nullable = false, columnDefinition = "CHAR(1)")
    private String ativo;

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "sensorOrbital", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LeituraSatelite> leituras = new ArrayList<>();
}