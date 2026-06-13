package com.tallersoft.service;

import com.tallersoft.dto.RepuestoRequest;
import com.tallersoft.dto.RepuestoResponse;
import com.tallersoft.exception.EntityNotFoundException;
import com.tallersoft.exception.InsufficientStockException;
import com.tallersoft.mapper.RepuestoMapper;
import com.tallersoft.model.Repuesto;
import com.tallersoft.repository.RepuestoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepuestoService {
    
    private final RepuestoRepository repuestoRepository;
    private final RepuestoMapper repuestoMapper;
    
    @Transactional
    public RepuestoResponse crearRepuesto(RepuestoRequest request) {
        log.info("Creando nuevo repuesto: {}", request.getNombre());
        
        Repuesto repuesto = repuestoMapper.toEntity(request);
        Repuesto saved = repuestoRepository.save(repuesto);
        
        log.info("Repuesto creado con id: {}", saved.getId());
        
        RepuestoResponse response = repuestoMapper.toResponse(saved);
        response.setCritico(repuestoMapper.calculateCritico(saved));
        response.setBajo(repuestoMapper.calculateBajo(saved));
        return response;
    }

    @Transactional(readOnly = true)
    public RepuestoResponse obtenerRepuesto(Long id) {
        Repuesto repuesto = repuestoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Repuesto no encontrado"));

        RepuestoResponse response = repuestoMapper.toResponse(repuesto);
        response.setCritico(repuestoMapper.calculateCritico(repuesto));
        response.setBajo(repuestoMapper.calculateBajo(repuesto));
        return response;
    }

    @Transactional(readOnly = true)
    public List<RepuestoResponse> listarRepuestos() {
        List<Repuesto> repuestos = repuestoRepository.findByActivoTrue();
        return repuestos.stream()
                .map(r -> {
                    RepuestoResponse response = repuestoMapper.toResponse(r);
                    response.setCritico(repuestoMapper.calculateCritico(r));
                    response.setBajo(repuestoMapper.calculateBajo(r));
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RepuestoResponse> listarRepuestosCriticos() {
        List<Repuesto> repuestos = repuestoRepository.findRepuestosCriticos();
        return repuestos.stream()
                .map(r -> {
                    RepuestoResponse response = repuestoMapper.toResponse(r);
                    response.setCritico(true);
                    response.setBajo(false);
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public RepuestoResponse editarRepuesto(Long id, RepuestoRequest request) {
        log.info("Editando repuesto {}", id);

        Repuesto repuesto = repuestoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Repuesto no encontrado"));

        repuesto.setNombre(request.getNombre());
        repuesto.setCategoria(request.getCategoria());
        repuesto.setPrecio(request.getPrecio());
        repuesto.setStockActual(request.getStockActual());
        repuesto.setStockMinimo(request.getStockMinimo());
        repuesto.setStockBajo(request.getStockBajo());

        Repuesto updated = repuestoRepository.save(repuesto);

        RepuestoResponse response = repuestoMapper.toResponse(updated);
        response.setCritico(repuestoMapper.calculateCritico(updated));
        response.setBajo(repuestoMapper.calculateBajo(updated));
        return response;
    }
    
    @Transactional
    public void darDeBajaRepuesto(Long id) {
        log.info("Dando de baja repuesto {}", id);
        Repuesto repuesto = repuestoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Repuesto no encontrado"));
        repuesto.setActivo(false);
        repuestoRepository.save(repuesto);
        log.info("Repuesto {} dado de baja: {}", id, repuesto.getNombre());
    }

    @Transactional
    public void reactivarRepuesto(Long id) {
        log.info("Reactivando repuesto {}", id);
        Repuesto repuesto = repuestoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Repuesto no encontrado"));
        repuesto.setActivo(true);
        repuestoRepository.save(repuesto);
        log.info("Repuesto {} reactivado: {}", id, repuesto.getNombre());
    }

    @Transactional(readOnly = true)
    public List<RepuestoResponse> listarTodosRepuestos() {
        List<Repuesto> repuestos = repuestoRepository.findAll();
        return repuestos.stream()
                .map(r -> {
                    RepuestoResponse response = repuestoMapper.toResponse(r);
                    response.setCritico(repuestoMapper.calculateCritico(r));
                    response.setBajo(repuestoMapper.calculateBajo(r));
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void decrementarStock(Long repuestoId, Integer cantidad) {
        log.info("Decrementando stock del repuesto {} en {}", repuestoId, cantidad);
        
        Repuesto repuesto = repuestoRepository.findById(repuestoId)
                .orElseThrow(() -> new EntityNotFoundException("Repuesto no encontrado"));
        
        Integer nuevoStock = repuesto.getStockActual() - cantidad;
        
        if (nuevoStock < 0) {
            throw new InsufficientStockException(
                    String.format("Stock insuficiente. Stock actual: %d, se intenta restar: %d",
                            repuesto.getStockActual(), cantidad)
            );
        }
        
        repuesto.setStockActual(nuevoStock);
        repuestoRepository.save(repuesto);
        
        log.info("Stock del repuesto {} decrementado a {}", repuestoId, nuevoStock);
    }
}
