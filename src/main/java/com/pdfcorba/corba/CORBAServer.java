package com.pdfcorba.corba;

import org.omg.CORBA.*;
import org.omg.PortableServer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Properties;

@Component
public class CORBAServer {

    private static final Logger log = LoggerFactory.getLogger(CORBAServer.class);

    @Value("${corba.server.port:1050}")
    private String corbaPort;

    @Value("${corba.server.host:localhost}")
    private String corbaHost;

    private ORB            orb;
    private POA            rootPOA;
    private Thread         orbThread;
    private PDFServiceImpl pdfServant;
    private String         iorReference;

    @PostConstruct
    public void startCORBAServer() {
        // Servant toujours créé en premier — jamais null
        pdfServant = new PDFServiceImpl();

        try {
            log.info("╔══════════════════════════════════════════╗");
            log.info("║     Démarrage du Serveur CORBA...        ║");
            log.info("╚══════════════════════════════════════════╝");

            // 1. Init ORB
            Properties props = new Properties();
            orb = ORB.init(new String[]{}, props);
            log.info("✔ ORB initialisé");

            // 2. Récupérer le POA racine
            rootPOA = POAHelper.narrow(
                orb.resolve_initial_references("RootPOA")
            );

            // 3. Activer le POA Manager
            rootPOA.the_POAManager().activate();
            log.info("✔ POA Manager activé");

            // 4. Activer le servant avec ID explicite
            byte[] oid = "PDFService".getBytes("UTF-8");
            rootPOA.activate_object_with_id(oid, pdfServant);
            log.info("✔ Servant activé dans le POA");

            // 5. IOR via create_reference_with_id (évite id_to_reference)
            org.omg.CORBA.Object ref = rootPOA.create_reference_with_id(
                oid, "IDL:PDFServiceModule/PDFService:1.0"
            );
            iorReference = orb.object_to_string(ref);
            log.info("✔ IOR générée ({} chars)", iorReference.length());

            // 6. ORB dans thread daemon
            orbThread = new Thread(() -> orb.run(), "CORBA-ORB-Thread");
            orbThread.setDaemon(true);
            orbThread.start();

            log.info("╔══════════════════════════════════════════╗");
            log.info("║   Serveur CORBA démarré avec succès !    ║");
            log.info("╚══════════════════════════════════════════╝");

        } catch (Exception e) {
            log.warn("⚠ CORBA en mode dégradé: {}", e.getMessage());
            iorReference = "N/A";
        }
    }

    @PreDestroy
    public void stopCORBAServer() {
        try {
            if (orb != null) {
                orb.shutdown(false);
                orb.destroy();
            }
        } catch (Exception ignored) {}
    }

    public PDFServiceImpl getPdfServant() {
        return pdfServant;
    }

    public String getIorReference() {
        return iorReference != null ? iorReference : "N/A";
    }

    public boolean isRunning() {
        return orbThread != null && orbThread.isAlive();
    }

    public String getServerInfo() {
        return "CORBA | Host: " + corbaHost + " | Port: " + corbaPort
             + " | Status: " + (isRunning() ? "✔ RUNNING" : "⚠ DEGRADED");
    }
}