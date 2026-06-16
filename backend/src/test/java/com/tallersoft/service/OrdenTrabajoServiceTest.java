package com.tallersoft.service;

import com.tallersoft.dto.*;
import com.tallersoft.exception.*;
import com.tallersoft.mapper.OrdenTrabajoMapper;
import com.tallersoft.mapper.OrdenRepuestoMapper;
import com.tallersoft.model.*;
import com.tallersoft.repository.*;
import com.tallersoft.service.KanbanNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdenTrabajoServiceTest {
    
    @Mock
    private Authentication adminAuth;

    @Mock
    private OrdenTrabajoRepository ordenTrabajoRepository;
    
    @Mock
    private RepuestoRepository repuestoRepository;
    
    @Mock
    private OrdenRepuestoRepository ordenRepuestoRepository;
    
    @Mock
    private EquipoRepository equipoRepository;
    
    @Mock
    private ClienteRepository clienteRepository;
    
    @Mock
    private UsuarioRepository usuarioRepository;
    
    @Mock
    private OrdenTrabajoMapper ordenTrabajoMapper;
    
    @Mock
    private OrdenRepuestoMapper ordenRepuestoMapper;

    @Mock
    private KanbanNotificationService kanbanNotificationService;

    @InjectMocks
    private OrdenTrabajoService ordenTrabajoService;
    
    private Cliente cliente;
    private Equipo equipo;
    private Usuario tecnico;
    private Repuesto repuesto;
    private OrdenTrabajo orden;
    
    @BeforeEach
    void setUp() {
        when(adminAuth.getAuthorities()).thenAnswer(inv ->
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        cliente = Cliente.builder().id(1L).nombre("Cliente Test").activo(true).build();
        equipo = Equipo.builder().id(1L).cliente(cliente).tipo("Equipo Test").build();
        tecnico = Usuario.builder().id(1L).nombre("Tecnico Test").build();
        repuesto = Repuesto.builder()
                .id(1L)
                .nombre("Repuesto Test")
                .precio(BigDecimal.TEN)
                .stockActual(10)
                .stockMinimo(5)
                .build();
        orden = OrdenTrabajo.builder()
                .id(1L)
                .cliente(cliente)
                .equipo(equipo)
                .tecnico(tecnico)
                .fallaReportada("Falla de prueba")
                .estado(EstadoOrden.PENDIENTE)
                .prioridad(Prioridad.NORMAL)
                .presupuesto(BigDecimal.ZERO)
                .build();
    }
    
    @Test
    void crearOrden_debeCrearConEstadoPendiente() {
        // Arrange
        OrdenTrabajoRequest request = new OrdenTrabajoRequest();
        request.setEquipoId(1L);
        request.setClienteId(1L);
        request.setFallaReportada("Falla de prueba");
        
        when(equipoRepository.findById(1L)).thenReturn(Optional.of(equipo));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(ordenTrabajoRepository.save(any(OrdenTrabajo.class))).thenReturn(orden);
        when(ordenTrabajoMapper.toResponse(orden)).thenReturn(new OrdenTrabajoResponse());
        
        // Act
        OrdenTrabajoResponse resultado = ordenTrabajoService.crearOrden(request);
        
        // Assert
        verify(ordenTrabajoRepository, times(1)).save(argThat(o -> 
                o.getEstado() == EstadoOrden.PENDIENTE && 
                o.getPresupuesto().equals(BigDecimal.ZERO)
        ));
        assertNotNull(resultado);
    }
    
    @Test
    void cambiarEstado_aPendienteDesdeEnProceso_debefallar() {
        // Arrange
        orden.setEstado(EstadoOrden.EN_PROCESO);
        when(ordenTrabajoRepository.findById(1L)).thenReturn(Optional.of(orden));
        
        // Act & Assert
        assertThrows(InvalidStateTransitionException.class, () ->
                ordenTrabajoService.cambiarEstado(1L, EstadoOrden.PENDIENTE, adminAuth)
        );
    }
    
    @Test
    void cambiarEstado_aListoSinDiagnostico_debeLanzarMissingDiagnosticException() {
        // Arrange
        orden.setEstado(EstadoOrden.EN_PROCESO);
        orden.setDiagnostico(null);
        when(ordenTrabajoRepository.findById(1L)).thenReturn(Optional.of(orden));
        
        // Act & Assert
        assertThrows(MissingDiagnosticException.class, () ->
                ordenTrabajoService.cambiarEstado(1L, EstadoOrden.LISTO, adminAuth)
        );
    }
    
    @Test
    void cambiarEstado_aListoConDiagnostico_debeExito() {
        // Arrange
        orden.setEstado(EstadoOrden.EN_PROCESO);
        orden.setDiagnostico("Diagnóstico de prueba");
        when(ordenTrabajoRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(ordenTrabajoRepository.save(any(OrdenTrabajo.class))).thenReturn(orden);
        when(ordenTrabajoMapper.toResponse(orden)).thenReturn(new OrdenTrabajoResponse());
        
        // Act
        OrdenTrabajoResponse resultado = ordenTrabajoService.cambiarEstado(1L, EstadoOrden.LISTO, adminAuth);
        
        // Assert
        verify(ordenTrabajoRepository, times(1)).save(argThat(o -> o.getEstado() == EstadoOrden.LISTO));
        assertNotNull(resultado);
    }
    
    @Test
    void agregarRepuesto_conStockSuficiente_debeDecrementarStock() {
        // Arrange
        when(ordenTrabajoRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(repuestoRepository.findByIdWithLock(1L)).thenReturn(Optional.of(repuesto));
        when(ordenRepuestoRepository.findByOrdenId(1L)).thenReturn(List.of());
        when(ordenRepuestoRepository.save(any(OrdenRepuesto.class))).thenReturn(new OrdenRepuesto());
        when(ordenTrabajoRepository.save(any(OrdenTrabajo.class))).thenReturn(orden);
        when(ordenTrabajoMapper.toResponse(orden)).thenReturn(new OrdenTrabajoResponse());
        
        AgregarRepuestoRequest request = new AgregarRepuestoRequest();
        request.setRepuestoId(1L);
        request.setCantidad(5);
        
        // Act
        ordenTrabajoService.agregarRepuesto(1L, request);
        
        // Assert
        verify(repuestoRepository, times(1)).save(argThat(r -> r.getStockActual() == 5));
    }
    
    @Test
    void agregarRepuesto_sinStockSuficiente_debeLanzarInsufficientStockException() {
        // Arrange
        repuesto.setStockActual(2);
        when(ordenTrabajoRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(repuestoRepository.findByIdWithLock(1L)).thenReturn(Optional.of(repuesto));
        
        AgregarRepuestoRequest request = new AgregarRepuestoRequest();
        request.setRepuestoId(1L);
        request.setCantidad(5);
        
        // Act & Assert
        assertThrows(InsufficientStockException.class, () ->
                ordenTrabajoService.agregarRepuesto(1L, request)
        );
    }
    
    @Test
    void agregarRepuesto_debeRecalcularPresupuesto() {
        // Arrange
        when(ordenTrabajoRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(repuestoRepository.findByIdWithLock(1L)).thenReturn(Optional.of(repuesto));
        
        OrdenRepuesto ordenRepuesto = OrdenRepuesto.builder()
                .cantidad(5)
                .precioUnit(BigDecimal.TEN)
                .build();
        
        when(ordenRepuestoRepository.findByOrdenId(1L)).thenReturn(List.of(ordenRepuesto));
        when(ordenRepuestoRepository.save(any(OrdenRepuesto.class))).thenReturn(ordenRepuesto);
        when(ordenTrabajoRepository.save(any(OrdenTrabajo.class))).thenReturn(orden);
        when(ordenTrabajoMapper.toResponse(orden)).thenReturn(new OrdenTrabajoResponse());
        
        AgregarRepuestoRequest request = new AgregarRepuestoRequest();
        request.setRepuestoId(1L);
        request.setCantidad(5);
        
        // Act
        ordenTrabajoService.agregarRepuesto(1L, request);
        
        // Assert
        verify(ordenTrabajoRepository, times(1)).save(argThat(o -> 
                o.getPresupuesto().equals(new BigDecimal("50")) // 5 * 10
        ));
    }
}
