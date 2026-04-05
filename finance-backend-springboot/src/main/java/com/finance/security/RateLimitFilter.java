package com.finance.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple sliding-window rate limiter: 100 requests per 15 minutes per IP.
 * Uses an in-memory ConcurrentHashMap — sufficient for a single-instance app.
 * For multi-instance deployments, replace with Redis-backed rate limiting.
 */
@Component
public class RateLimitFilter implements Filter {

    private static final int    MAX_REQUESTS  = 100;
    private static final long   WINDOW_MS     = 15 * 60 * 1000L; // 15 minutes

    private final Map<String, long[]> requestLog = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        // Skip rate limiting for H2 console in dev
        if (request.getRequestURI().startsWith("/h2-console")) {
            chain.doFilter(req, res);
            return;
        }

        String ip = resolveClientIp(request);
        long   now = System.currentTimeMillis();

        requestLog.compute(ip, (key, window) -> {
            if (window == null || now - window[0] > WINDOW_MS) {
                return new long[]{ now, 1 }; // [windowStart, count]
            }
            window[1]++;
            return window;
        });

        long[] window = requestLog.get(ip);
        long remaining = MAX_REQUESTS - window[1];

        response.setHeader("X-RateLimit-Limit",     String.valueOf(MAX_REQUESTS));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, remaining)));
        response.setHeader("X-RateLimit-Reset",     String.valueOf(window[0] + WINDOW_MS));

        if (window[1] > MAX_REQUESTS) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                """
                {"error":"Too many requests. Limit is %d per 15 minutes.","status":429}
                """.formatted(MAX_REQUESTS)
            );
            return;
        }

        chain.doFilter(req, res);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
