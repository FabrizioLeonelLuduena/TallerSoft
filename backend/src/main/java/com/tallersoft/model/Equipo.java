package com.tallersoft.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

/**
 * Equipo Entity - Represents customer equipment/device
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "equipos")
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;

    @Column(name = "marca", length = 50)
    private String marca;

    @Column(name = "modelo", length = 100)
    private String modelo;

    @Column(name = "numero_serie", length = 100)
    private String numeroSerie;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
}
