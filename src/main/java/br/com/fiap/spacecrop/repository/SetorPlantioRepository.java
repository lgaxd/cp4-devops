package br.com.fiap.spacecrop.repository;

import br.com.fiap.spacecrop.entity.SetorPlantio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SetorPlantioRepository extends JpaRepository<SetorPlantio, Long> {

    Page<SetorPlantio> findByFazendaId(Long fazendaId, Pageable pageable);

    Optional<SetorPlantio> findByIdAndFazendaId(Long id, Long fazendaId);

    boolean existsByIdAndFazendaId(Long id, Long fazendaId);

    @Query("SELECT s FROM SetorPlantio s WHERE s.fazenda.id = :fazendaId AND " +
            "(:cultura IS NULL OR LOWER(s.cultura) LIKE LOWER(CONCAT('%', :cultura, '%')))")
    Page<SetorPlantio> findByFazendaIdWithFilters(@Param("fazendaId") Long fazendaId,
            @Param("cultura") String cultura,
            Pageable pageable);
}