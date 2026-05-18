package com.tallersoft.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cliente Entity - Represents a workshop client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "direccion", length = 200)
    private String direccion;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Equipo> equipos;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
