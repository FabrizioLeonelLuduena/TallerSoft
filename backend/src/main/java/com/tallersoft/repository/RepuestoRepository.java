package com.tallersoft.repository;

import com.tallersoft.model.Repuesto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepuestoRepository extends JpaRepository<Repuesto, Long> {
    List<Repuesto> findByActivoTrue();

    List<Repuesto> findByStockActualLessThanEqual(Integer stockMinimo);

    List<Repuesto> findByNombreContainingIgnoreCase(String nombre);

    List<Repuesto> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);

    @Query("SELECT r FROM Repuesto r WHERE r.stockActual <= r.stockMinimo AND r.activo = true")
    List<Repuesto> findRepuestosCriticos();

    @Query("SELECT r FROM Repuesto r WHERE r.stockActual > r.stockMinimo AND r.stockActual <= r.stockBajo AND r.activo = true")
    List<Repuesto> findRepuestosBajo();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Repuesto r WHERE r.id = :id")
    Optional<Repuesto> findByIdWithLock(Long id);
}
