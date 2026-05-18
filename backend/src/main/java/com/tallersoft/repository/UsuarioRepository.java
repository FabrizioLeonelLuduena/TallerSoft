package com.tallersoft.repository;

import com.tallersoft.model.Rol;
import com.tallersoft.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Usuario entity
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    Optional<Usuario> findByEmail(String email);
    
    List<Usuario> findByRol(Rol rol);
    
    List<Usuario> findByRolAndActivoTrue(Rol rol);
    
    boolean existsByEmail(String email);
}
