package com.tallersoft.service;

import com.tallersoft.dto.RepuestoRequest;
import com.tallersoft.dto.RepuestoResponse;
import com.tallersoft.exception.EntityNotFoundException;
import com.tallersoft.exception.InsufficientStockException;
import com.tallersoft.mapper.RepuestoMapper;
import com.tallersoft.model.Repuesto;
import com.tallersoft.repository.RepuestoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepuestoServiceTest {
    
    @Mock
    private RepuestoRepository repuestoRepository;
    
    @Mock
    private RepuestoMapper repuestoMapper;
    
    @InjectMocks
    private RepuestoService repuestoService;
    
    private Repuesto repuesto;
    
    @BeforeEach
    void setUp() {
        repuesto = Repuesto.builder()
                .id(1L)
                .nombre("Repuesto Test")
                .categoria("Electrónica")
                .precio(BigDecimal.valueOf(100))
                .stockActual(10)
                .stockMinimo(5)
                .build();
    }
    
    @Test
    void crearRepuesto_debeGuardarCorrectamente() {
        // Arrange
        RepuestoRequest request = new RepuestoRequest();
        request.setNombre("Repuesto Test");
        request.setCategoria("Electrónica");
        request.setPrecio(BigDecimal.valueOf(100));
        request.setStockActual(10);
        request.setStockMinimo(5);
        
        when(repuestoMapper.toEntity(request)).thenReturn(repuesto);
        when(repuestoRepository.save(any(Repuesto.class))).thenReturn(repuesto);
        when(repuestoMapper.toResponse(repuesto)).thenReturn(new RepuestoResponse());
        when(repuestoMapper.calculateCritico(repuesto)).thenReturn(false);
        
        // Act
        RepuestoResponse resultado = repuestoService.crearRepuesto(request);
        
        // Assert
        verify(repuestoRepository, times(1)).save(any(Repuesto.class));
        assertNotNull(resultado);
    }
    
    @Test
    void listarRepuestosCriticos_debeRetornarSoloConStockBajo() {
        // Arrange
        Repuesto repuestoCritico = Repuesto.builder()
                .id(2L)
                .nombre("Repuesto Crítico")
                .stockActual(3)
                .stockMinimo(5)
                .build();
        
        when(repuestoRepository.findRepuestosCriticos()).thenReturn(List.of(repuestoCritico));
        when(repuestoMapper.toResponse(repuestoCritico)).thenReturn(new RepuestoResponse());
        
        // Act
        List<RepuestoResponse> resultado = repuestoService.listarRepuestosCriticos();
        
        // Assert
        verify(repuestoRepository, times(1)).findRepuestosCriticos();
        assertEquals(1, resultado.size());
    }
    
    @Test
    void decrementarStock_conStockInsuficiente_debeLanzarExcepcion() {
        // Arrange
        repuesto.setStockActual(2);
        when(repuestoRepository.findById(1L)).thenReturn(Optional.of(repuesto));
        
        // Act & Assert
        assertThrows(InsufficientStockException.class, () ->
                repuestoService.decrementarStock(1L, 5)
        );
    }
    
    @Test
    void editarRepuesto_noExistente_debeLanzarEntityNotFoundException() {
        // Arrange
        when(repuestoRepository.findById(99L)).thenReturn(Optional.empty());
        
        RepuestoRequest request = new RepuestoRequest();
        
        // Act & Assert
        assertThrows(EntityNotFoundException.class, () ->
                repuestoService.editarRepuesto(99L, request)
        );
    }
}
