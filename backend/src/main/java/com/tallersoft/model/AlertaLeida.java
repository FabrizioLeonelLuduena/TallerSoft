package com.tallersoft.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "alertas_leidas",
    uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "alerta_key"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertaLeida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "alerta_key", nullable = false)
    private String alertaKey;

    @Column(name = "leida_en", nullable = false, updatable = false)
    private LocalDateTime leidaEn;

    @PrePersist
    protected void onCreate() {
        this.leidaEn = LocalDateTime.now();
    }
}
