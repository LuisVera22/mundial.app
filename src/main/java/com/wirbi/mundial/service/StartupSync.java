package com.wirbi.mundial.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Sync opcional al arranque (app.sync.on-startup=true). Útil para poblar la BD
 * (p. ej. Atlas) sin hacer el POST manual: seteas MONGODB_URI + FOOTBALL_DATA_TOKEN
 * + SYNC_ON_STARTUP=true y al iniciar se trae el calendario real. Corre DESPUÉS
 * del seed (Order 2). No bloquea el arranque si falla.
 */
@Component
@Order(2)
public class StartupSync implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupSync.class);

    private final SyncService sync;
    private final boolean enabled;

    public StartupSync(SyncService sync, @Value("${app.sync.on-startup:false}") boolean enabled) {
        this.sync = sync;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Sync on-startup OFF (app.sync.on-startup=false). Usa POST /api/admin/sync.");
            return;
        }
        try {
            SyncResult r = sync.sync();
            log.info("Sync on-startup: {}", r);
        } catch (Exception e) {
            log.warn("Sync on-startup falló (no bloquea el arranque): {}", e.getMessage());
        }
    }
}
