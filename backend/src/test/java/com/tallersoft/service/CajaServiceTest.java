package com.tallersoft.service;

import com.tallersoft.dto.CobrarOrdenRequest;
import com.tallersoft.dto.CobroResponse;
import com.tallersoft.exception.CobrosException;
import com.tallersoft.exception.EntityNotFoundException;
import com.tallersoft.mapper.CobroMapper;
import com.tallersoft.model.*;
import com.tallersoft.repository.CobrosRepository;
import com.tallersoft.repository.OrdenTrabajoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CajaServiceTest {

    @Mock private CobrosRepository cobrosRepository;
    @Mock private OrdenTrabajoRepository ordenTrabajoRepository;
    @Mock private MercadoPagoService mercadoPagoService;
    @Mock private CobroMapper cobroMapper;
    @InjectMocks private CobrosService cobrosService;

    private OrdenTrabajo ordenLista;
    private CobroResponse cobroResponseMock;

    @BeforeEach
    void setUp() {
        ordenLista = OrdenTrabajo.builder()
                .id(10L)
                .estado(EstadoOrden.LISTO)
                .presupuesto(new BigDecimal("5000.00"))
                .build();

        cobroResponseMock = new CobroResponse();
    }

    // ─── registrarCobro EFECTIVO ─────────────────────────────────────────────

    @Test
    @DisplayName("registrarCobro EFECTIVO: pasa a APROBADO inmediatamente y calcula vuelto")
    void registrarCobro_efectivo_deberiaAprobarYCalcularVuelto() {
        CobrarOrdenRequest request = new CobrarOrdenRequest();
        request.setOrdenId(10L);
        request.setMonto(new BigDecimal("5000.00"));
        request.setMedioPago(MedioPago.EFECTIVO);
        request.setMontoRecibido(new BigDecimal("6000.00"));

        when(ordenTrabajoRepository.findById(10L)).thenReturn(Optional.of(ordenLista));
        when(cobrosRepository.findByOrdenIdAndEstadoPago(10L, EstadoPago.APROBADO))
                .thenReturn(Optional.empty());
        when(cobrosRepository.save(any(Cobro.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ordenTrabajoRepository.save(any(OrdenTrabajo.class))).thenReturn(ordenLista);
        when(cobroMapper.toResponse(any(Cobro.class))).thenReturn(cobroResponseMock);

        cobrosService.registrarCobro(request);

        verify(cobrosRepository, times(1)).save(argThat(c ->
                c.getEstadoPago() == EstadoPago.APROBADO &&
                c.getVuelto().compareTo(new BigDecimal("1000.00")) == 0
        ));
        verify(ordenTrabajoRepository, times(1)).save(argThat(o ->
                o.getEstado() == EstadoOrden.ENTREGADO
        ));
    }

    @Test
    @DisplayName("registrarCobro EFECTIVO: monto recibido insuficiente → lanza CobrosException")
    void registrarCobro_efectivo_montoInsuficiente_deberiaLanzarExcepcion() {
        CobrarOrdenRequest request = new CobrarOrdenRequest();
        request.setOrdenId(10L);
        request.setMonto(new BigDecimal("5000.00"));
        request.setMedioPago(MedioPago.EFECTIVO);
        request.setMontoRecibido(new BigDecimal("3000.00"));

        when(ordenTrabajoRepository.findById(10L)).thenReturn(Optional.of(ordenLista));
        when(cobrosRepository.findByOrdenIdAndEstadoPago(10L, EstadoPago.APROBADO))
                .thenReturn(Optional.empty());

        assertThrows(CobrosException.class, () -> cobrosService.registrarCobro(request));
        verify(cobrosRepository, never()).save(any());
    }

    // ─── registrarCobro TARJETA ──────────────────────────────────────────────

    @Test
    @DisplayName("registrarCobro TARJETA: pasa a APROBADO directamente")
    void registrarCobro_tarjeta_deberiaAprobarInmediatamente() {
        CobrarOrdenRequest request = new CobrarOrdenRequest();
        request.setOrdenId(10L);
        request.setMonto(new BigDecimal("5000.00"));
        request.setMedioPago(MedioPago.TARJETA);

        when(ordenTrabajoRepository.findById(10L)).thenReturn(Optional.of(ordenLista));
        when(cobrosRepository.findByOrdenIdAndEstadoPago(10L, EstadoPago.APROBADO))
                .thenReturn(Optional.empty());
        when(cobrosRepository.save(any(Cobro.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ordenTrabajoRepository.save(any(OrdenTrabajo.class))).thenReturn(ordenLista);
        when(cobroMapper.toResponse(any(Cobro.class))).thenReturn(cobroResponseMock);

        cobrosService.registrarCobro(request);

        verify(cobrosRepository, times(1)).save(argThat(c ->
                c.getEstadoPago() == EstadoPago.APROBADO &&
                c.getMedioPago() == MedioPago.TARJETA
        ));
    }

    // ─── registrarCobro MERCADOPAGO ──────────────────────────────────────────

    @Test
    @DisplayName("registrarCobro MERCADOPAGO: queda PENDIENTE hasta webhook")
    void registrarCobro_mercadoPago_deberiaQuedaPendiente() {
        CobrarOrdenRequest request = new CobrarOrdenRequest();
        request.setOrdenId(10L);
        request.setMonto(new BigDecimal("5000.00"));
        request.setMedioPago(MedioPago.MERCADOPAGO);

        when(ordenTrabajoRepository.findById(10L)).thenReturn(Optional.of(ordenLista));
        when(cobrosRepository.findByOrdenIdAndEstadoPago(10L, EstadoPago.APROBADO))
                .thenReturn(Optional.empty());
        when(mercadoPagoService.cargarOrdenEnCaja(eq(10L), any(), any()))
                .thenReturn("https://qr.example.com/image.png");
        when(cobrosRepository.save(any(Cobro.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cobroMapper.toResponse(any(Cobro.class))).thenReturn(cobroResponseMock);

        cobrosService.registrarCobro(request);

        verify(cobrosRepository, times(1)).save(argThat(c ->
                c.getEstadoPago() == EstadoPago.PENDIENTE &&
                c.getMedioPago() == MedioPago.MERCADOPAGO
        ));
        // Orden NO debe cambiar a ENTREGADO mientras el pago está pendiente
        verify(ordenTrabajoRepository, never()).save(any());
    }

    // ─── idempotencia webhook ────────────────────────────────────────────────

    @Test
    @DisplayName("registrarCobro: orden ya con cobro APROBADO → lanza CobrosException (idempotencia)")
    void registrarCobro_ordenYaAprobada_deberiaLanzarExcepcion() {
        CobrarOrdenRequest request = new CobrarOrdenRequest();
        request.setOrdenId(10L);
        request.setMonto(new BigDecimal("5000.00"));
        request.setMedioPago(MedioPago.EFECTIVO);
        request.setMontoRecibido(new BigDecimal("5000.00"));

        when(ordenTrabajoRepository.findById(10L)).thenReturn(Optional.of(ordenLista));
        when(cobrosRepository.findByOrdenIdAndEstadoPago(10L, EstadoPago.APROBADO))
                .thenReturn(Optional.of(new Cobro()));

        assertThrows(CobrosException.class, () -> cobrosService.registrarCobro(request));
        verify(cobrosRepository, never()).save(any());
    }

    // ─── registrarCobro: orden no en LISTO ───────────────────────────────────

    @Test
    @DisplayName("registrarCobro: orden no en LISTO → lanza CobrosException")
    void registrarCobro_ordenNoLista_deberiaLanzarExcepcion() {
        ordenLista.setEstado(EstadoOrden.EN_PROCESO);
        CobrarOrdenRequest request = new CobrarOrdenRequest();
        request.setOrdenId(10L);
        request.setMonto(new BigDecimal("5000.00"));
        request.setMedioPago(MedioPago.EFECTIVO);

        when(ordenTrabajoRepository.findById(10L)).thenReturn(Optional.of(ordenLista));

        assertThrows(CobrosException.class, () -> cobrosService.registrarCobro(request));
    }

    // ─── procesarPagoAprobadoMercadoPago ────────────────────────────────────

    @Test
    @DisplayName("procesarPagoAprobadoMercadoPago: pago approved → cobro APROBADO y orden ENTREGADA")
    void procesarPagoAprobado_deberiaActualizarCobroYOrden() {
        Cobro cobro = Cobro.builder()
                .id(5L)
                .orden(ordenLista)
                .monto(new BigDecimal("5000.00"))
                .medioPago(MedioPago.MERCADOPAGO)
                .estadoPago(EstadoPago.PENDIENTE)
                .build();

        when(mercadoPagoService.consultarPago("mp_payment_001"))
                .thenReturn(java.util.Map.of(
                        "status", "approved",
                        "externalReference", "10",
                        "mpPaymentId", "mp_payment_001"
                ));
        when(cobrosRepository.findByMpPaymentId("mp_payment_001")).thenReturn(Optional.of(cobro));
        when(cobrosRepository.save(any(Cobro.class))).thenReturn(cobro);
        when(ordenTrabajoRepository.save(any(OrdenTrabajo.class))).thenReturn(ordenLista);

        cobrosService.procesarPagoAprobadoMercadoPago("mp_payment_001");

        verify(cobrosRepository, times(1)).save(argThat(c ->
                c.getEstadoPago() == EstadoPago.APROBADO
        ));
        verify(ordenTrabajoRepository, times(1)).save(argThat(o ->
                o.getEstado() == EstadoOrden.ENTREGADO
        ));
    }

    @Test
    @DisplayName("procesarPagoAprobadoMercadoPago: cobro no encontrado → lanza EntityNotFoundException")
    void procesarPagoAprobado_cobroNoEncontrado_deberiaLanzarExcepcion() {
        when(mercadoPagoService.consultarPago("unknown_id"))
                .thenReturn(java.util.Map.of(
                        "status", "approved",
                        "externalReference", "",
                        "mpPaymentId", "unknown_id"
                ));
        when(cobrosRepository.findByMpPaymentId("unknown_id")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> cobrosService.procesarPagoAprobadoMercadoPago("unknown_id"));
    }

    // ─── confirmarPagoManual ─────────────────────────────────────────────────

    @Test
    @DisplayName("confirmarPagoManual: cobro ya APROBADO → lanza CobrosException")
    void confirmarPagoManual_yaAprobado_deberiaLanzarExcepcion() {
        Cobro cobro = Cobro.builder()
                .id(5L)
                .orden(ordenLista)
                .medioPago(MedioPago.TARJETA)
                .estadoPago(EstadoPago.APROBADO)
                .build();

        when(cobrosRepository.findById(5L)).thenReturn(Optional.of(cobro));

        assertThrows(CobrosException.class,
                () -> cobrosService.confirmarPagoManual(5L));
    }
}
