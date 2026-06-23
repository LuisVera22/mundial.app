package com.wirbi.mundial.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting de /api/** (token bucket en memoria, por instancia):
 * cada cliente — IP del primer salto de X-Forwarded-For — tiene una ráfaga
 * máxima y una tasa sostenida por minuto; al excederla recibe 429 con
 * Retry-After. Protege contra scraping y abuso accidental; los ataques de
 * credenciales los absorbe Microsoft Entra ID (el login no pasa por aquí).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    /** Tasa sostenida (requests/min por cliente) y ráfaga máxima. */
    private final double refillPerSecond;
    private final int capacity;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${app.rate-limit.per-minute:120}") int perMinute) {
        this.refillPerSecond = perMinute / 60.0;
        this.capacity = perMinute;
    }

    private final class Bucket {
        private double tokens = capacity;
        private long lastNanos = System.nanoTime();

        synchronized boolean tryConsume() {
            long now = System.nanoTime();
            tokens = Math.min(capacity, tokens + (now - lastNanos) / 1_000_000_000.0 * refillPerSecond);
            lastNanos = now;
            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }

        synchronized boolean idleFor(long nanos) {
            return System.nanoTime() - lastNanos > nanos;
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        evictIdleIfCrowded();
        Bucket bucket = buckets.computeIfAbsent(clientKey(request), k -> new Bucket());
        if (!bucket.tryConsume()) {
            response.setStatus(429);
            response.setHeader("Retry-After", "30");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Demasiadas solicitudes. Intenta de nuevo en unos segundos.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    /** Cliente = primera IP de X-Forwarded-For (el LB la fija) o la remota. */
    private String clientKey(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** Evita crecimiento sin tope del mapa: purga buckets inactivos (>10 min). */
    private void evictIdleIfCrowded() {
        if (buckets.size() > 10_000) {
            long tenMinutes = 600L * 1_000_000_000L;
            buckets.entrySet().removeIf(e -> e.getValue().idleFor(tenMinutes));
        }
    }
}
