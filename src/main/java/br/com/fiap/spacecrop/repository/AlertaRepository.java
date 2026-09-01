package br.com.fiap.spacecrop.repository;

import br.com.fiap.spacecrop.entity.Alerta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    Page<Alerta> findByLeituraFazendaId(Long fazendaId, Pageable pageable);

    @Query("SELECT a FROM Alerta a WHERE " +
            "(:resolvido IS NULL OR a.resolvido = :resolvido) AND " +
            "(:idTipoAlerta IS NULL OR a.idTipoAlerta = :idTipoAlerta) AND " +
            "(:fazendaId IS NULL OR a.leitura.fazenda.id = :fazendaId)")
    Page<Alerta> findAllWithFilters(@Param("resolvido") Character resolvido,
            @Param("idTipoAlerta") Long idTipoAlerta,
            @Param("fazendaId") Long fazendaId,
            Pageable pageable);
}