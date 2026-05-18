package com.tallersoft.gateway.filter;

import com.tallersoft.gateway.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global JWT Validation Filter for Spring Cloud Gateway
 * Validates JWT tokens for all requests
 * Allows public endpoints without JWT
 */
@Slf4j
@Component
public class JwtValidationFilter implements GlobalFilter {

    @Autowired
    private JwtUtil jwtUtil;

    // Public endpoints that don't require JWT
    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/login",
            "/auth/register",
            "/api/pagos/webhook",
            "/swagger-ui",
            "/v3/api-docs"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Check if endpoint is public
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        // Extract JWT token from Authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        
        if (authHeader == null || authHeader.isEmpty()) {
            log.warn("Missing Authorization header for path: {}", path);
            return setErrorResponse(exchange, HttpStatus.UNAUTHORIZED, 
                    "Authorization header requerido");
        }

        try {
            // Validate token format
            if (!authHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Invalid Authorization header format");
            }

            String token = authHeader.substring(7);

            // Validate JWT
            if (!jwtUtil.validateToken(token)) {
                log.warn("Invalid or expired token for path: {}", path);
                return setErrorResponse(exchange, HttpStatus.UNAUTHORIZED, 
                        "Token inválido o expirado");
            }

            log.debug("JWT validation successful for path: {}", path);
            return chain.filter(exchange);

        } catch (Exception ex) {
            log.error("Error validating JWT: ", ex);
            return setErrorResponse(exchange, HttpStatus.UNAUTHORIZED, 
                    "Error validando token");
        }
    }

    /**
     * Check if the endpoint is public (doesn't require JWT)
     */
    private boolean isPublicEndpoint(String path) {
        for (String publicEndpoint : PUBLIC_ENDPOINTS) {
            if (path.startsWith(publicEndpoint)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set error response
     */
    private Mono<Void> setErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory()
                        .wrap(("{\"error\":\"" + message + "\"}").getBytes()))
        );
    }
}
