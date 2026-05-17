package com.pdfcorba;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application - Point d'entrée Spring Boot
 * 
 * Lance simultanément :
 *   - Le serveur CORBA (via @PostConstruct de CORBAServer)
 *   - Le serveur web Spring Boot (Tomcat embarqué)
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════╗");
        System.out.println("║         CORBA PDF SERVICE - Démarrage             ║");
        System.out.println("║   Architecture Distribuée avec Java CORBA + PDFBox║");
        System.out.println("╚═══════════════════════════════════════════════════╝");

        SpringApplication.run(Application.class, args);
    }
}
