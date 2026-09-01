package br.com.fiap.spacecrop.repository;

import br.com.fiap.spacecrop.entity.LeituraSatelite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LeituraSateliteRepository extends JpaRepository<LeituraSatelite, Long> {

    Page<LeituraSatelite> findByFazendaId(Long fazendaId, Pageable pageable);

    @Query("SELECT l FROM LeituraSatelite l WHERE l.fazenda.id = :fazendaId " +
            "ORDER BY l.dataLeitura DESC")
    List<LeituraSatelite> findTop10ByFazendaIdOrderByDataLeituraDesc(@Param("fazendaId") Long fazendaId);

    @Query("SELECT l FROM LeituraSatelite l WHERE l.fazenda.id = :fazendaId AND " +
            "(:dataInicio IS NULL OR l.dataLeitura >= :dataInicio) AND " +
            "(:dataFim IS NULL OR l.dataLeitura <= :dataFim) AND " +
            "(:anomalia IS NULL OR l.anomalia = :anomalia)")
    Page<LeituraSatelite> findByFazendaIdWithFilters(@Param("fazendaId") Long fazendaId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("anomalia") String anomalia,
            Pageable pageable);
}