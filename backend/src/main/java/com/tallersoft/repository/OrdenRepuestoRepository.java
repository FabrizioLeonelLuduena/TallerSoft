package com.tallersoft.repository;

import com.tallersoft.model.OrdenRepuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdenRepuestoRepository extends JpaRepository<OrdenRepuesto, Long> {
    List<OrdenRepuesto> findByOrdenId(Long ordenId);
    
    List<OrdenRepuesto> findByRepuestoId(Long repuestoId);
}
