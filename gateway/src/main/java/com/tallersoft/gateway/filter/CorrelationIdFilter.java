package com.tallersoft.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Genera o propaga el header X-Correlation-ID en cada request que pasa por el gateway.
 * Si el cliente no lo envía, se genera uno nuevo. El mismo ID se incluye en la respuesta
 * para que el frontend pueda correlacionar request/response en los logs del cliente.
 * Se ejecuta antes que todos los demás filtros (order = -2).
 */
@Slf4j
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        final String finalId = correlationId;

        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.header(CORRELATION_ID_HEADER, finalId))
                .build();

        mutated.getResponse().beforeCommit(() -> {
            mutated.getResponse().getHeaders().set(CORRELATION_ID_HEADER, finalId);
            return Mono.empty();
        });

        log.debug("Correlation-ID: {} → {}", finalId, exchange.getRequest().getPath().value());
        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return -2; // Antes del JwtValidationFilter (que tiene orden 0 por defecto)
    }
}
