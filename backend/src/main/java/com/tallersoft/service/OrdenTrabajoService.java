package com.tallersoft.service;

import com.tallersoft.dto.*;
import com.tallersoft.exception.*;
import com.tallersoft.mapper.OrdenTrabajoMapper;
import com.tallersoft.mapper.OrdenRepuestoMapper;
import com.tallersoft.model.*;
import com.tallersoft.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrdenTrabajoService {
    
    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final RepuestoRepository repuestoRepository;
    private final OrdenRepuestoRepository ordenRepuestoRepository;
    private final EquipoRepository equipoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrdenTrabajoMapper ordenTrabajoMapper;
    private final OrdenRepuestoMapper ordenRepuestoMapper;
    
    @Transactional
    public OrdenTrabajoResponse crearOrden(OrdenTrabajoRequest request) {
        log.info("Creando nueva orden de trabajo");
        
        Equipo equipo = equipoRepository.findById(request.getEquipoId())
                .orElseThrow(() -> new EntityNotFoundException("Equipo no encontrado"));
        
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));
        
        Usuario tecnico = null;
        if (request.getTecnicoId() != null) {
            tecnico = usuarioRepository.findById(request.getTecnicoId())
                    .orElseThrow(() -> new EntityNotFoundException("Técnico no encontrado"));
        }
        
        OrdenTrabajo orden = OrdenTrabajo.builder()
                .equipo(equipo)
                .cliente(cliente)
                .tecnico(tecnico)
                .fallaReportada(request.getFallaReportada())
                .estado(EstadoOrden.PENDIENTE)
                .prioridad(request.getPrioridad() != null ? request.getPrioridad() : Prioridad.NORMAL)
                .presupuesto(BigDecimal.ZERO)
                .build();
        
        OrdenTrabajo saved = ordenTrabajoRepository.save(orden);
        log.info("Orden creada con id: {}", saved.getId());
        return ordenTrabajoMapper.toResponse(saved);
    }
    
    @Transactional(readOnly = true)
    public OrdenTrabajoResponse obtenerOrden(Long id) {
        OrdenTrabajo orden = ordenTrabajoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada"));
        return ordenTrabajoMapper.toResponse(orden);
    }
    
    @Transactional(readOnly = true)
    public List<OrdenTrabajoResponse> listarOrdenes(EstadoOrden estado, Long tecnicoId, LocalDate desde, LocalDate hasta) {
        List<OrdenTrabajo> ordenes;
        
        if (estado != null && tecnicoId != null) {
            ordenes = ordenTrabajoRepository.findByTecnicoIdAndEstado(tecnicoId, estado);
        } else if (tecnicoId != null) {
            ordenes = ordenTrabajoRepository.findByTecnicoId(tecnicoId);
        } else if (estado != null) {
            ordenes = ordenTrabajoRepository.findByEstado(estado);
        } else if (desde != null && hasta != null) {
            LocalDateTime startOfDay = desde.atStartOfDay();
            LocalDateTime endOfDay = hasta.atTime(LocalTime.MAX);
            ordenes = ordenTrabajoRepository.findByCreatedAtBetween(startOfDay, endOfDay);
        } else {
            ordenes = ordenTrabajoRepository.findAll();
        }
        
        return ordenes.stream()
                .map(ordenTrabajoMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<OrdenTrabajoResponse> listarOrdenesPorCliente(Long clienteId) {
        return ordenTrabajoRepository.findByClienteIdOrderByCreatedAtDesc(clienteId).stream()
                .map(ordenTrabajoMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrdenTrabajoResponse> listarOrdenesActivas() {
        List<OrdenTrabajo> ordenes = ordenTrabajoRepository.findByEstadoNot(EstadoOrden.ENTREGADO);
        return ordenes.stream()
                .map(ordenTrabajoMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public OrdenTrabajoResponse cambiarEstado(Long id, EstadoOrden nuevoEstado) {
        log.info("Cambiando estado de orden {} a {}", id, nuevoEstado);
        
        OrdenTrabajo orden = ordenTrabajoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada"));
        
        EstadoOrden estadoActual = orden.getEstado();
        
        // Validar transiciones válidas
        boolean transicionValida = false;
        if (estadoActual == EstadoOrden.PENDIENTE && nuevoEstado == EstadoOrden.EN_PROCESO) {
            transicionValida = true;
        } else if (estadoActual == EstadoOrden.EN_PROCESO && nuevoEstado == EstadoOrden.LISTO) {
            transicionValida = true;
        } else if (estadoActual == EstadoOrden.LISTO && nuevoEstado == EstadoOrden.ENTREGADO) {
            transicionValida = true;
        }
        
        if (!transicionValida) {
            throw new InvalidStateTransitionException(
                    String.format("Transición inválida de %s a %s", estadoActual, nuevoEstado)
            );
        }
        
        // Si el nuevo estado es LISTO, verificar que existe diagnóstico
        if (nuevoEstado == EstadoOrden.LISTO) {
            if (orden.getDiagnostico() == null || orden.getDiagnostico().isBlank()) {
                throw new MissingDiagnosticException("No se puede cambiar a LISTO sin diagnóstico");
            }
        }
        
        orden.setEstado(nuevoEstado);
        OrdenTrabajo updated = ordenTrabajoRepository.save(orden);
        log.info("Estado de orden {} cambiado a {}", id, nuevoEstado);
        return ordenTrabajoMapper.toResponse(updated);
    }
    
    @Transactional
    public OrdenTrabajoResponse agregarDiagnostico(Long id, String diagnostico) {
        log.info("Agregando diagnóstico a orden {}", id);
        
        OrdenTrabajo orden = ordenTrabajoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada"));
        
        if (orden.getEstado() == EstadoOrden.ENTREGADO) {
            throw new InvalidStateTransitionException("No se puede agregar diagnóstico a orden entregada");
        }
        
        orden.setDiagnostico(diagnostico);
        OrdenTrabajo updated = ordenTrabajoRepository.save(orden);
        return ordenTrabajoMapper.toResponse(updated);
    }
    
    @Transactional
    public OrdenTrabajoResponse agregarRepuesto(Long ordenId, AgregarRepuestoRequest request) {
        log.info("Agregando repuesto {} a orden {}", request.getRepuestoId(), ordenId);
        
        OrdenTrabajo orden = ordenTrabajoRepository.findById(ordenId)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada"));

        if (orden.getEstado() == EstadoOrden.ENTREGADO) {
            throw new InvalidStateTransitionException(
                    "No se puede agregar repuestos a una orden ya entregada");
        }

        Repuesto repuesto = repuestoRepository.findById(request.getRepuestoId())
                .orElseThrow(() -> new EntityNotFoundException("Repuesto no encontrado"));
        
        // Verificar stock disponible
        if (repuesto.getStockActual() < request.getCantidad()) {
            throw new InsufficientStockException(
                    String.format("Stock insuficiente. Disponible: %d, Solicitado: %d",
                            repuesto.getStockActual(), request.getCantidad())
            );
        }
        
        // Decrementar stock
        repuesto.setStockActual(repuesto.getStockActual() - request.getCantidad());
        repuestoRepository.save(repuesto);
        
        // Crear OrdenRepuesto con precio snapshot
        OrdenRepuesto ordenRepuesto = OrdenRepuesto.builder()
                .orden(orden)
                .repuesto(repuesto)
                .cantidad(request.getCantidad())
                .precioUnit(repuesto.getPrecio())
                .build();
        
        ordenRepuestoRepository.save(ordenRepuesto);
        
        // Recalcular presupuesto
        List<OrdenRepuesto> repuestos = ordenRepuestoRepository.findByOrdenId(ordenId);
        BigDecimal nuevoPresupuesto = repuestos.stream()
                .map(or -> or.getPrecioUnit().multiply(new BigDecimal(or.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        orden.setPresupuesto(nuevoPresupuesto);
        OrdenTrabajo updated = ordenTrabajoRepository.save(orden);
        
        log.info("Repuesto agregado a orden {}, nuevo presupuesto: {}", ordenId, nuevoPresupuesto);
        return ordenTrabajoMapper.toResponse(updated);
    }
    
    @Transactional
    public OrdenTrabajoResponse eliminarRepuesto(Long ordenId, Long ordenRepuestoId) {
        log.info("Eliminando repuesto {} de orden {}", ordenRepuestoId, ordenId);

        OrdenTrabajo orden = ordenTrabajoRepository.findById(ordenId)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada"));

        if (orden.getEstado() == EstadoOrden.ENTREGADO) {
            throw new InvalidStateTransitionException("No se puede modificar una orden ya entregada");
        }

        OrdenRepuesto ordenRepuesto = ordenRepuestoRepository.findById(ordenRepuestoId)
                .orElseThrow(() -> new EntityNotFoundException("Item de repuesto no encontrado"));

        // Devolver stock al inventario
        Repuesto repuesto = ordenRepuesto.getRepuesto();
        repuesto.setStockActual(repuesto.getStockActual() + ordenRepuesto.getCantidad());
        repuestoRepository.save(repuesto);

        // Eliminar de la colección en memoria antes de deletar en BD
        // (evita que el caché de Hibernate de primer nivel devuelva el ítem en el response)
        orden.getRepuestos().removeIf(r -> r.getId().equals(ordenRepuestoId));
        ordenRepuestoRepository.deleteById(ordenRepuestoId);

        // Recalcular presupuesto con la colección ya actualizada
        BigDecimal nuevoPresupuesto = orden.getRepuestos().stream()
                .map(or -> or.getPrecioUnit().multiply(new BigDecimal(or.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        orden.setPresupuesto(nuevoPresupuesto);
        OrdenTrabajo updated = ordenTrabajoRepository.save(orden);

        log.info("Repuesto eliminado de orden {}, nuevo presupuesto: {}", ordenId, nuevoPresupuesto);
        return ordenTrabajoMapper.toResponse(updated);
    }

    @Transactional
    public void actualizarEstadoAEntregado(Long ordenId) {
        log.info("Actualizando orden {} a estado ENTREGADO", ordenId);
        
        OrdenTrabajo orden = ordenTrabajoRepository.findById(ordenId)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada"));
        
        orden.setEstado(EstadoOrden.ENTREGADO);
        ordenTrabajoRepository.save(orden);
        
        log.info("Orden {} marcada como entregada", ordenId);
    }
}
