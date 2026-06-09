package com.tallersoft.service;

import com.tallersoft.dto.CajaDiariaResponse;
import com.tallersoft.dto.CobrarOrdenRequest;
import com.tallersoft.dto.CobroResponse;
import com.tallersoft.exception.CobrosException;
import com.tallersoft.exception.EntityNotFoundException;
import com.tallersoft.mapper.CobroMapper;
import com.tallersoft.model.*;
import com.tallersoft.repository.CobrosRepository;
import com.tallersoft.repository.OrdenTrabajoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CobrosService {

    private final CobrosRepository cobrosRepository;
    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final MercadoPagoService mercadoPagoService;
    private final CobroMapper cobroMapper;

    @Transactional
    public CobroResponse registrarCobro(CobrarOrdenRequest request) {
        log.info("Registrando cobro para orden {}, medio: {}", request.getOrdenId(), request.getMedioPago());

        OrdenTrabajo orden = ordenTrabajoRepository.findById(request.getOrdenId())
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada con id: " + request.getOrdenId()));

        if (orden.getEstado() != EstadoOrden.LISTO) {
            throw new CobrosException(
                    "Solo se puede cobrar una orden en estado LISTO. Estado actual: " + orden.getEstado());
        }

        cobrosRepository.findByOrdenIdAndEstadoPago(request.getOrdenId(), EstadoPago.APROBADO)
                .ifPresent(c -> {
                    throw new CobrosException("La orden ya tiene un cobro aprobado");
                });

        Cobro cobro = Cobro.builder()
                .orden(orden)
                .monto(request.getMonto())
                .medioPago(request.getMedioPago())
                .estadoPago(EstadoPago.PENDIENTE)
                .build();

        switch (request.getMedioPago()) {
            case EFECTIVO -> {
                if (request.getMontoRecibido() == null) {
                    throw new CobrosException("El monto recibido es requerido para pago en efectivo");
                }
                if (request.getMontoRecibido().compareTo(request.getMonto()) < 0) {
                    throw new CobrosException(
                            String.format("Monto recibido insuficiente. Requerido: %.2f, Recibido: %.2f",
                                    request.getMonto(), request.getMontoRecibido()));
                }
                cobro.setMontoRecibido(request.getMontoRecibido());
                cobro.setVuelto(request.getMontoRecibido().subtract(request.getMonto()));
                cobro.setEstadoPago(EstadoPago.APROBADO);
                cobrosRepository.save(cobro);
                orden.setEstado(EstadoOrden.ENTREGADO);
                ordenTrabajoRepository.save(orden);
                log.info("Cobro EFECTIVO aprobado para orden {}, vuelto: {}", orden.getId(), cobro.getVuelto());
            }
            case TARJETA -> {
                cobro.setEstadoPago(EstadoPago.APROBADO);
                cobrosRepository.save(cobro);
                orden.setEstado(EstadoOrden.ENTREGADO);
                ordenTrabajoRepository.save(orden);
                log.info("Cobro TARJETA aprobado para orden {}", orden.getId());
            }
            case MERCADOPAGO -> {
                String desc = "Reparación orden #" + orden.getId();
                String qrImageUrl = mercadoPagoService.cargarOrdenEnCaja(orden.getId(), request.getMonto(), desc);
                cobro.setMpQrImageUrl(qrImageUrl);
                cobrosRepository.save(cobro);
                log.info("Cobro MERCADOPAGO pendiente creado para orden {}", orden.getId());
            }
        }

        return cobroMapper.toResponse(cobro);
    }

    @Transactional
    public CobroResponse getCobro(Long cobroId) {
        Cobro cobro = cobrosRepository.findById(cobroId)
                .orElseThrow(() -> new EntityNotFoundException("Cobro no encontrado con id: " + cobroId));

        // Reconciliación activa: si el cobro MP sigue PENDIENTE, consultar MP directamente
        if (cobro.getMedioPago() == MedioPago.MERCADOPAGO
                && cobro.getEstadoPago() == EstadoPago.PENDIENTE) {
            try {
                mercadoPagoService.buscarPagoPorOrden(cobro.getOrden().getId())
                        .filter(r -> "approved".equals(r.get("status")))
                        .ifPresent(r -> {
                            cobro.setEstadoPago(EstadoPago.APROBADO);
                            cobro.setMpPaymentId((String) r.get("mpPaymentId"));
                            cobrosRepository.save(cobro);
                            OrdenTrabajo orden = cobro.getOrden();
                            orden.setEstado(EstadoOrden.ENTREGADO);
                            ordenTrabajoRepository.save(orden);
                            log.info("Cobro {} actualizado a APROBADO via reconciliación activa (MP payment: {})",
                                    cobroId, r.get("mpPaymentId"));
                        });
            } catch (Exception e) {
                log.warn("Error en reconciliación activa para cobro {}: {}", cobroId, e.getMessage());
            }
        }

        return cobroMapper.toResponse(cobro);
    }

    @Transactional
    public CobroResponse confirmarPagoManual(Long cobroId) {
        Cobro cobro = cobrosRepository.findById(cobroId)
                .orElseThrow(() -> new EntityNotFoundException("Cobro no encontrado con id: " + cobroId));

        if (cobro.getEstadoPago() == EstadoPago.APROBADO) {
            throw new CobrosException("El cobro ya está aprobado");
        }
        if (cobro.getMedioPago() == MedioPago.MERCADOPAGO) {
            throw new CobrosException("Los pagos de MercadoPago se confirman via webhook");
        }

        cobro.setEstadoPago(EstadoPago.APROBADO);
        cobrosRepository.save(cobro);

        OrdenTrabajo orden = cobro.getOrden();
        orden.setEstado(EstadoOrden.ENTREGADO);
        ordenTrabajoRepository.save(orden);

        log.info("Cobro {} confirmado manualmente", cobroId);
        return cobroMapper.toResponse(cobro);
    }

    @Transactional
    public void procesarPagoAprobadoMercadoPago(String mpPaymentId) {
        log.info("Procesando pago MercadoPago: {}", mpPaymentId);

        Map<String, Object> pagoInfo = mercadoPagoService.consultarPago(mpPaymentId);
        String status = (String) pagoInfo.get("status");
        String externalReference = (String) pagoInfo.get("externalReference");

        Cobro cobro = cobrosRepository.findByMpPaymentId(mpPaymentId)
                .or(() -> {
                    if (externalReference != null && !externalReference.isBlank()) {
                        Long ordenId = Long.parseLong(externalReference);
                        return cobrosRepository.findByOrdenId(ordenId);
                    }
                    return java.util.Optional.empty();
                })
                .orElseThrow(() -> new EntityNotFoundException("Cobro no encontrado para payment id: " + mpPaymentId));

        cobro.setMpPaymentId(mpPaymentId);

        if ("approved".equals(status)) {
            cobro.setEstadoPago(EstadoPago.APROBADO);
            cobrosRepository.save(cobro);

            OrdenTrabajo orden = cobro.getOrden();
            orden.setEstado(EstadoOrden.ENTREGADO);
            ordenTrabajoRepository.save(orden);
            log.info("Pago MP aprobado para orden {}", orden.getId());
        } else if ("rejected".equals(status)) {
            cobro.setEstadoPago(EstadoPago.RECHAZADO);
            cobrosRepository.save(cobro);
            log.info("Pago MP rechazado, payment id: {}", mpPaymentId);
        } else {
            cobrosRepository.save(cobro);
        }
    }

    @Transactional(readOnly = true)
    public CajaDiariaResponse getCajaDiaria(LocalDate fecha) {
        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.atTime(LocalTime.MAX);

        List<Cobro> cobros = cobrosRepository.findByEstadoPagoAndCreatedAtBetween(
                EstadoPago.APROBADO, desde, hasta);

        BigDecimal totalEfectivo = sumarPorMedio(cobros, MedioPago.EFECTIVO);
        BigDecimal totalTarjeta = sumarPorMedio(cobros, MedioPago.TARJETA);
        BigDecimal totalMercadoPago = sumarPorMedio(cobros, MedioPago.MERCADOPAGO);
        BigDecimal totalDia = totalEfectivo.add(totalTarjeta).add(totalMercadoPago);

        CajaDiariaResponse response = new CajaDiariaResponse();
        response.setFecha(fecha);
        response.setTotalDia(totalDia);
        response.setCantidadOrdenes(cobros.size());
        response.setTotalEfectivo(totalEfectivo);
        response.setTotalTarjeta(totalTarjeta);
        response.setTotalMercadoPago(totalMercadoPago);
        response.setCobrosDelDia(cobros.stream().map(cobroMapper::toResponse).collect(Collectors.toList()));

        return response;
    }

    @Transactional(readOnly = true)
    public List<CajaDiariaResponse> getHistorialCajas(int anio, int mes) {
        LocalDate inicio = LocalDate.of(anio, mes, 1);
        LocalDate fin    = inicio.withDayOfMonth(inicio.lengthOfMonth());

        List<Cobro> cobros = cobrosRepository.findByEstadoPagoAndCreatedAtBetween(
                EstadoPago.APROBADO, inicio.atStartOfDay(), fin.atTime(LocalTime.MAX));

        return cobros.stream()
                .collect(Collectors.groupingBy(c -> c.getCreatedAt().toLocalDate()))
                .entrySet().stream()
                .map(e -> buildCajaDiariaFromList(e.getKey(), e.getValue()))
                .sorted((a, b) -> b.getFecha().compareTo(a.getFecha()))
                .collect(Collectors.toList());
    }

    private CajaDiariaResponse buildCajaDiariaFromList(LocalDate fecha, List<Cobro> cobros) {
        BigDecimal totalEfectivo    = sumarPorMedio(cobros, MedioPago.EFECTIVO);
        BigDecimal totalTarjeta     = sumarPorMedio(cobros, MedioPago.TARJETA);
        BigDecimal totalMercadoPago = sumarPorMedio(cobros, MedioPago.MERCADOPAGO);

        CajaDiariaResponse response = new CajaDiariaResponse();
        response.setFecha(fecha);
        response.setTotalDia(totalEfectivo.add(totalTarjeta).add(totalMercadoPago));
        response.setCantidadOrdenes(cobros.size());
        response.setTotalEfectivo(totalEfectivo);
        response.setTotalTarjeta(totalTarjeta);
        response.setTotalMercadoPago(totalMercadoPago);
        response.setCobrosDelDia(cobros.stream().map(cobroMapper::toResponse).collect(Collectors.toList()));
        return response;
    }

    private BigDecimal sumarPorMedio(List<Cobro> cobros, MedioPago medio) {
        return cobros.stream()
                .filter(c -> c.getMedioPago() == medio)
                .map(Cobro::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
