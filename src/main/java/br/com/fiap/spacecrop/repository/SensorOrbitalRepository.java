package br.com.fiap.spacecrop.repository;

import br.com.fiap.spacecrop.entity.SensorOrbital;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SensorOrbitalRepository extends JpaRepository<SensorOrbital, Long> {

    Page<SensorOrbital> findBySateliteId(Long sateliteId, Pageable pageable);

    @Query("SELECT s FROM SensorOrbital s WHERE " +
            "(:nome IS NULL OR LOWER(s.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
            "(:ativo IS NULL OR s.ativo = :ativo)")
    Page<SensorOrbital> findAllWithFilters(@Param("nome") String nome,
            @Param("ativo") String ativo,
            Pageable pageable);
}