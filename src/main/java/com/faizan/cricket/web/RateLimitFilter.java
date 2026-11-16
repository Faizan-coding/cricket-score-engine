package com.faizan.cricket.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Fixed-window limiter (per client IP, per second) on write requests. Reads are not limited. */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final class Window {
        private long startMs = System.currentTimeMillis();
        private int count;

        synchronized boolean tryAcquire(int limit) {
            long now = System.currentTimeMillis();
            if (now - startMs >= 1000) {
                startMs = now;
                count = 0;
            }
            return ++count <= limit;
        }
    }

    private final int limitPerSecond;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${cricket.rate-limit.per-second:50}") int limitPerSecond) {
        this.limitPerSecond = limitPerSecond;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        Window w = windows.computeIfAbsent(req.getRemoteAddr(), k -> new Window());
        if (!w.tryAcquire(limitPerSecond)) {
            res.setStatus(429);
            res.setHeader("Retry-After", "1");
            res.setContentType("application/json");
            res.getWriter().write("{\"status\":429,\"error\":\"Rate limit exceeded\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}
