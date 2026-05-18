package com.tallersoft.repository;

import com.tallersoft.model.Repuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepuestoRepository extends JpaRepository<Repuesto, Long> {
    List<Repuesto> findByStockActualLessThanEqual(Integer stockMinimo);
    
    List<Repuesto> findByNombreContainingIgnoreCase(String nombre);
    
    @Query("SELECT r FROM Repuesto r WHERE r.stockActual <= r.stockMinimo")
    List<Repuesto> findRepuestosCriticos();
}
