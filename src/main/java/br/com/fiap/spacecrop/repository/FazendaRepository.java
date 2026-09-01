package br.com.fiap.spacecrop.repository;

import br.com.fiap.spacecrop.entity.Fazenda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FazendaRepository extends JpaRepository<Fazenda, Long> {

        Page<Fazenda> findByUsuarioId(Long usuarioId, Pageable pageable);

        Optional<Fazenda> findByIdAndUsuarioId(Long id, Long usuarioId);

        boolean existsByIdAndUsuarioId(Long id, Long usuarioId);

        @Query("SELECT f FROM Fazenda f WHERE f.usuario.id = :usuarioId AND " +
                        "(:nome IS NULL OR LOWER(f.nome) LIKE LOWER(CONCAT('%', :nome, '%')))")
        Page<Fazenda> findByUsuarioIdWithFilters(@Param("usuarioId") Long usuarioId,
                        @Param("nome") String nome,
                        Pageable pageable);

        @Query("SELECT f FROM Fazenda f WHERE " +
                        "(:nome IS NULL OR LOWER(f.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
                        "(:estado IS NULL OR f.estado = :estado)")
        Page<Fazenda> findAllWithFilters(@Param("nome") String nome,
                        @Param("estado") String estado,
                        Pageable pageable);
}