package com.tallersoft.model;

import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "repuestos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Repuesto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 150, nullable = false)
    private String nombre;
    
    @Column(length = 80)
    private String categoria;
    
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal precio;
    
    @Column(nullable = false)
    private Integer stockActual = 0;

    @Column(nullable = false)
    private Integer stockMinimo = 5;

    @Column(nullable = false)
    private Integer stockBajo = 10;
    
    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
