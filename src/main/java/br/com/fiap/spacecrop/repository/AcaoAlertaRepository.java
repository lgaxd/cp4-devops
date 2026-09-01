package br.com.fiap.spacecrop.repository;

import br.com.fiap.spacecrop.entity.AcaoAlerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcaoAlertaRepository extends JpaRepository<AcaoAlerta, Long> {
    List<AcaoAlerta> findByAlertaId(Long alertaId);
}