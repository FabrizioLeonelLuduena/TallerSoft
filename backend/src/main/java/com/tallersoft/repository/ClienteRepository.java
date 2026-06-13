package com.tallersoft.repository;

import com.tallersoft.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Cliente entity
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    
    List<Cliente> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);

    List<Cliente> findByNombreContainingIgnoreCase(String nombre);

    List<Cliente> findByTelefonoContainingAndActivoTrue(String telefono);

    List<Cliente> findByActivoTrue();
}
