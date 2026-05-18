package com.tallersoft.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TallerSoft API Gateway - Main entry point for Spring Cloud Gateway
 * 
 * This is the API Gateway that routes all client requests to appropriate backend services:
 * - /api/** → Core Service (port 8081)
 * - /auth/** → Core Service (port 8081)
 * - /analytics/** → Analytics Service (port 8082)
 * - /swagger-ui/** → Core Service (port 8081)
 * 
 * Gateway runs on port 8080 and is the only public entry point.
 * 
 * @author TallerSoft Team
 * @version 1.0.0
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
