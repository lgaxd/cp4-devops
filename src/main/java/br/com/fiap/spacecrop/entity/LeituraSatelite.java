package br.com.fiap.spacecrop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "TB_LEITURA_SATELITE")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class LeituraSatelite {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LEITURA")
    @SequenceGenerator(name = "SEQ_LEITURA", sequenceName = "SEQ_LEITURA", allocationSize = 1)
    @Column(name = "id_leitura")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sensor_orbital", nullable = false)
    private SensorOrbital sensorOrbital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_fazenda", nullable = false)
    private Fazenda fazenda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_setor")
    private SetorPlantio setor;

    @Column(name = "nr_valor", nullable = false, columnDefinition = "NUMBER(8,2)")
    private Double valor;

    @Column(name = "dt_leitura", nullable = false)
    private LocalDateTime dataLeitura;

    @Column(name = "fl_anomalia", nullable = false, columnDefinition = "CHAR(1)")
    private String anomalia;

    @JsonIgnore
    @OneToOne(mappedBy = "leitura", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Alerta alerta;

}