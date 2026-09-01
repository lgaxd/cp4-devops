package br.com.fiap.spacecrop.repository;

import br.com.fiap.spacecrop.entity.Satelite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SateliteRepository extends JpaRepository<Satelite, Long> {

    @Query("SELECT s FROM Satelite s WHERE " +
            "(:nome IS NULL OR LOWER(s.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
            "(:operador IS NULL OR LOWER(s.operador) LIKE LOWER(CONCAT('%', :operador, '%')))")
    Page<Satelite> findAllWithFilters(@Param("nome") String nome,
            @Param("operador") String operador,
            Pageable pageable);
}