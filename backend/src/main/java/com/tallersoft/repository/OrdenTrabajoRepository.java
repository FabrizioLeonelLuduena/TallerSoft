package com.tallersoft.repository;

import com.tallersoft.model.OrdenTrabajo;
import com.tallersoft.model.EstadoOrden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Long> {
    List<OrdenTrabajo> findByEstado(EstadoOrden estado);
    
    List<OrdenTrabajo> findByTecnicoIdAndEstado(Long tecnicoId, EstadoOrden estado);
    
    List<OrdenTrabajo> findByTecnicoId(Long tecnicoId);
    
    List<OrdenTrabajo> findByClienteIdOrderByCreatedAtDesc(Long clienteId);
    
    List<OrdenTrabajo> findByCreatedAtBetween(LocalDateTime inicio, LocalDateTime fin);
    
    List<OrdenTrabajo> findByEstadoNot(EstadoOrden estado);
}
