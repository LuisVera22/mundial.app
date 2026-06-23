package com.wirbi.mundial.controller;

import com.wirbi.mundial.service.SyncResult;
import com.wirbi.mundial.service.SyncService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Endpoint de sincronización para el cron (Cloud Scheduler), que no puede
 * obtener un JWT de Entra. Protegido por secreto compartido en el header
 * {@code X-Sync-Token} (comparación en tiempo constante). Si {@code SYNC_TOKEN}
 * no está configurado, el endpoint queda deshabilitado.
 */
@RestController
@RequestMapping("/internal")
public class InternalSyncController {

    public static final String TOKEN_HEADER = "X-Sync-Token";

    private final SyncService sync;
    private final String token;

    public InternalSyncController(SyncService sync, @Value("${app.sync.token:}") String token) {
        this.sync = sync;
        this.token = token;
    }

    @PostMapping("/sync")
    public ResponseEntity<SyncResult> run(
            @RequestHeader(value = TOKEN_HEADER, required = false) String header) {
        if (token == null || token.isBlank() || header == null
                || !MessageDigest.isEqual(
                        token.getBytes(StandardCharsets.UTF_8),
                        header.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(sync.sync());
    }
}
