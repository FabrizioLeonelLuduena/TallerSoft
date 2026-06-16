package com.tallersoft.repository;

import com.tallersoft.model.AlertaLeida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaLeidaRepository extends JpaRepository<AlertaLeida, Long> {

    @Query("SELECT a.alertaKey FROM AlertaLeida a WHERE a.usuario.id = :usuarioId")
    List<String> findAlertaKeysByUsuarioId(Long usuarioId);

    boolean existsByUsuarioIdAndAlertaKey(Long usuarioId, String alertaKey);
}
