package com.tallersoft.repository;

import com.tallersoft.model.Cobro;
import com.tallersoft.model.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CobrosRepository extends JpaRepository<Cobro, Long> {

    Optional<Cobro> findByOrdenId(Long ordenId);

    Optional<Cobro> findByOrdenIdAndEstadoPago(Long ordenId, EstadoPago estado);

    Optional<Cobro> findByMpPaymentId(String mpPaymentId);

    List<Cobro> findByEstadoPagoAndCreatedAtBetween(
            EstadoPago estado, LocalDateTime desde, LocalDateTime hasta);
}
