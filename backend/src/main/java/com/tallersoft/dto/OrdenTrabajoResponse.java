package com.tallersoft.dto;

import com.tallersoft.model.EstadoOrden;
import com.tallersoft.model.Prioridad;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrdenTrabajoResponse {
    private Long id;
    private Long equipoId;
    private String equipoNombre;
    private String clienteNombre;
    private Long clienteId;
    private Long tecnicoId;
    private String tecnicoNombre;
    private String fallaReportada;
    private String diagnostico;
    private EstadoOrden estado;
    private Prioridad prioridad;
    private BigDecimal presupuesto;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrdenRepuestoResponse> repuestos;
}
