package com.tallersoft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TallerSoft Backend Application - Main entry point for Spring Boot Core Service
 * 
 * This is the core backend service for the TallerSoft ERP system.
 * Runs on port 8081 and provides REST APIs for work orders, clients, inventory, and payments.
 * 
 * @author TallerSoft Team
 * @version 1.0.0
 */
@SpringBootApplication
public class TallerSoftApplication {

    public static void main(String[] args) {
        SpringApplication.run(TallerSoftApplication.class, args);
    }

}
