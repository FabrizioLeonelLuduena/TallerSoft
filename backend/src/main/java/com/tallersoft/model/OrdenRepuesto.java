package com.tallersoft.model;

import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "orden_repuestos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenRepuesto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenTrabajo orden;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "repuesto_id", nullable = false)
    private Repuesto repuesto;
    
    @Column(nullable = false)
    private Integer cantidad;
    
    @Column(precision = 10, scale = 2, nullable = false, updatable = false)
    private BigDecimal precioUnit;
}
