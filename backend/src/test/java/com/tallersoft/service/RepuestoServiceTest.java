package com.tallersoft.service;

import com.tallersoft.dto.AgregarRepuestoRequest;
import com.tallersoft.dto.OrdenTrabajoResponse;
import com.tallersoft.dto.RepuestoRequest;
import com.tallersoft.dto.RepuestoResponse;
import com.tallersoft.exception.EntityNotFoundException;
import com.tallersoft.exception.InsufficientStockException;
import com.tallersoft.exception.InvalidStateTransitionException;
import com.tallersoft.mapper.OrdenRepuestoMapper;
import com.tallersoft.mapper.OrdenTrabajoMapper;
import com.tallersoft.mapper.RepuestoMapper;
import com.tallersoft.model.*;
import com.tallersoft.repository.*;
import com.tallersoft.service.KanbanNotificationService;
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

    // --- RepuestoService mocks ---
    @Mock
    private RepuestoRepository repuestoRepository;

    @Mock
    private RepuestoMapper repuestoMapper;

    @InjectMocks
    private RepuestoService repuestoService;

    // --- OrdenTrabajoService mocks (for agregarRepuestoAOrden tests) ---
    @Mock
    private OrdenTrabajoRepository ordenTrabajoRepository;

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

    private Repuesto repuesto;
    private OrdenTrabajo orden;

    @BeforeEach
    void setUp() {
        repuesto = Repuesto.builder()
                .id(1L)
                .nombre("Pantalla OLED iPhone 13")
                .categoria("Pantallas")
                .precio(BigDecimal.valueOf(45000))
                .stockActual(10)
                .stockMinimo(5)
                .build();

        orden = OrdenTrabajo.builder()
                .id(1L)
                .estado(EstadoOrden.EN_PROCESO)
                .presupuesto(BigDecimal.ZERO)
                .build();
    }

    // -----------------------------------------------------------------------
    // RepuestoService tests
    // -----------------------------------------------------------------------

    @Test
    void crearRepuesto_exitoso() {
        RepuestoRequest request = new RepuestoRequest();
        request.setNombre("Pantalla OLED iPhone 13");
        request.setCategoria("Pantallas");
        request.setPrecio(BigDecimal.valueOf(45000));
        request.setStockActual(10);
        request.setStockMinimo(5);

        when(repuestoMapper.toEntity(request)).thenReturn(repuesto);
        when(repuestoRepository.save(any(Repuesto.class))).thenReturn(repuesto);
        RepuestoResponse responseBase = new RepuestoResponse();
        when(repuestoMapper.toResponse(repuesto)).thenReturn(responseBase);
        when(repuestoMapper.calculateCritico(repuesto)).thenReturn(false);

        RepuestoResponse resultado = repuestoService.crearRepuesto(request);

        verify(repuestoRepository, times(1)).save(any(Repuesto.class));
        assertNotNull(resultado);
        assertFalse(resultado.getCritico());
    }

    @Test
    void editarRepuesto_exitoso() {
        RepuestoRequest request = new RepuestoRequest();
        request.setNombre("Pantalla OLED iPhone 14");
        request.setCategoria("Pantallas");
        request.setPrecio(BigDecimal.valueOf(50000));
        request.setStockActual(8);
        request.setStockMinimo(3);

        Repuesto actualizado = Repuesto.builder()
                .id(1L)
                .nombre("Pantalla OLED iPhone 14")
                .categoria("Pantallas")
                .precio(BigDecimal.valueOf(50000))
                .stockActual(8)
                .stockMinimo(3)
                .build();

        when(repuestoRepository.findById(1L)).thenReturn(Optional.of(repuesto));
        when(repuestoRepository.save(any(Repuesto.class))).thenReturn(actualizado);
        when(repuestoMapper.toResponse(actualizado)).thenReturn(new RepuestoResponse());
        when(repuestoMapper.calculateCritico(actualizado)).thenReturn(false);

        RepuestoResponse resultado = repuestoService.editarRepuesto(1L, request);

        verify(repuestoRepository, times(1)).save(any(Repuesto.class));
        assertNotNull(resultado);
    }

    @Test
    void listarRepuestos_soloStockCritico_retornaSoloCriticos() {
        Repuesto critico = Repuesto.builder()
                .id(2L)
                .nombre("Batería Samsung S21")
                .stockActual(2)
                .stockMinimo(5)
                .build();

        when(repuestoRepository.findRepuestosCriticos()).thenReturn(List.of(critico));
        RepuestoResponse responseBase = new RepuestoResponse();
        when(repuestoMapper.toResponse(critico)).thenReturn(responseBase);

        List<RepuestoResponse> resultado = repuestoService.listarRepuestosCriticos();

        verify(repuestoRepository, times(1)).findRepuestosCriticos();
        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).getCritico());
    }

    // -----------------------------------------------------------------------
    // agregarRepuestoAOrden tests (via OrdenTrabajoService)
    // -----------------------------------------------------------------------

    @Test
    void agregarRepuestoAOrden_stockSuficiente_descuentaStock() {
        AgregarRepuestoRequest request = new AgregarRepuestoRequest();
        request.setRepuestoId(1L);
        request.setCantidad(3);

        when(ordenTrabajoRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(repuestoRepository.findByIdWithLock(1L)).thenReturn(Optional.of(repuesto));
        when(ordenRepuestoRepository.save(any(OrdenRepuesto.class))).thenReturn(new OrdenRepuesto());
        when(ordenRepuestoRepository.findByOrdenId(1L)).thenReturn(List.of());
        when(ordenTrabajoRepository.save(any(OrdenTrabajo.class))).thenReturn(orden);
        when(ordenTrabajoMapper.toResponse(orden)).thenReturn(new OrdenTrabajoResponse());

        ordenTrabajoService.agregarRepuesto(1L, request);

        // stock 10 - 3 = 7
        verify(repuestoRepository, times(1)).save(argThat(r -> r.getStockActual() == 7));
    }

    @Test
    void agregarRepuestoAOrden_stockInsuficiente_lanzaExcepcion() {
        repuesto.setStockActual(2);
        AgregarRepuestoRequest request = new AgregarRepuestoRequest();
        request.setRepuestoId(1L);
        request.setCantidad(5);

        when(ordenTrabajoRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(repuestoRepository.findByIdWithLock(1L)).thenReturn(Optional.of(repuesto));

        assertThrows(InsufficientStockException.class, () ->
                ordenTrabajoService.agregarRepuesto(1L, request));
    }

    @Test
    void agregarRepuestoAOrden_ordenEntregada_lanzaExcepcion() {
        orden.setEstado(EstadoOrden.ENTREGADO);
        AgregarRepuestoRequest request = new AgregarRepuestoRequest();
        request.setRepuestoId(1L);
        request.setCantidad(1);

        when(ordenTrabajoRepository.findById(1L)).thenReturn(Optional.of(orden));

        assertThrows(InvalidStateTransitionException.class, () ->
                ordenTrabajoService.agregarRepuesto(1L, request));
    }
}
