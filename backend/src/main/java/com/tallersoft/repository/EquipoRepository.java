package com.tallersoft.repository;

import com.tallersoft.model.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Equipo entity
 */
@Repository
public interface EquipoRepository extends JpaRepository<Equipo, Long> {
    
    List<Equipo> findByClienteId(Long clienteId);
}
