package com.gestionstock.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int authLimit;
    private final int aiLimit;
    private final long windowMillis;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    public RateLimitingFilter(
            @Value("${stockpilot.rate-limit.auth-per-minute:20}") int authLimit,
            @Value("${stockpilot.rate-limit.ai-per-minute:40}") int aiLimit,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider
    ) {
        this.authLimit = authLimit;
        this.aiLimit = aiLimit;
        this.windowMillis = 60_000;
        this.redisTemplateProvider = redisTemplateProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        int limit = limitFor(path);
        if (limit > 0 && !allow(request, path, limit)) {
            response.setStatus(429);
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"title\":\"Too Many Requests\",\"detail\":\"Rate limit exceeded\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private int limitFor(String path) {
        if (path.startsWith("/auth/")) {
            return authLimit;
        }
        if (path.equals("/ai/copilot") || path.contains("/explanation") || path.startsWith("/ai/runs")) {
            return aiLimit;
        }
        return 0;
    }

    private boolean allow(HttpServletRequest request, String path, int limit) {
        String principal = request.getHeader("Authorization");
        String identity = principal == null || principal.isBlank() ? request.getRemoteAddr() : Integer.toString(principal.hashCode());
        String key = path + ":" + identity;
        Boolean redisAllowed = allowWithRedis(key, limit);
        if (redisAllowed != null) {
            return redisAllowed;
        }
        long now = Instant.now().toEpochMilli();
        Bucket bucket = buckets.compute(key, (ignored, current) -> {
            if (current == null || now - current.windowStart >= windowMillis) {
                return new Bucket(now, 1);
            }
            current.count++;
            return current;
        });
        return bucket.count <= limit;
    }

    private Boolean allowWithRedis(String key, int limit) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return null;
        }
        try {
            String redisKey = "rate-limit:" + Instant.now().getEpochSecond() / 60 + ":" + key;
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(75));
            }
            return count == null || count <= limit;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static final class Bucket {
        private final long windowStart;
        private int count;

        private Bucket(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
