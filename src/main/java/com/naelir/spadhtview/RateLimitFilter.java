package com.naelir.spadhtview;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Leaky-bucket rate limiter applied to all requests.
 *
 * <p>Each unique remote IP is allowed at most {@link #MAX_REQUESTS_PER_WINDOW}
 * requests within a sliding window of {@link #WINDOW_MS} milliseconds.
 * Requests that exceed the limit receive {@code 429 Too Many Requests}.
 * After {@link #BAN_THRESHOLD} violations the IP is permanently banned via
 * {@link IpRangeFilter#ban(String)} and subsequent requests get {@code 403 Forbidden}.
 *
 * <p>Configurable via system properties:
 * <ul>
 *   <li>{@code rate.limit}        – max requests per window per IP (default 10)</li>
 *   <li>{@code rate.limit.window} – window size in milliseconds (default 60 000 = 1 minute)</li>
 * </ul>
 */
@Provider
public class RateLimitFilter implements ContainerRequestFilter {

    private static final int  MAX_REQUESTS_PER_WINDOW =
            Integer.parseInt(System.getProperty("rate.limit", "10"));
    private static final long WINDOW_MS =
            Long.parseLong(System.getProperty("rate.limit.window", "60000"));
    private static final int  BAN_THRESHOLD = 3;

    /** Tracks [requestCount, windowStartMs] per IP. */
    private final ConcurrentHashMap<String, long[]>      buckets    = new ConcurrentHashMap<>();
    /** Tracks how many times each IP has exceeded the rate limit. */
    private final ConcurrentHashMap<String, AtomicInteger> violations = new ConcurrentHashMap<>();

    @Override
    public void filter(ContainerRequestContext ctx) {
        String ip = remoteIp(ctx);

        // Reject permanently banned IPs immediately
        try {
            byte[] ipBytes = InetAddress.getByName(ip).getAddress();
            if (IpRangeFilter.isDenied(ipBytes)) {
                ctx.abortWith(Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"error\":\"Forbidden\"}")
                        .build());
                return;
            }
        } catch (UnknownHostException ignored) {}

        long now = System.currentTimeMillis();

        buckets.compute(ip, (k, entry) -> {
            if (entry == null || now - entry[1] > WINDOW_MS) {
                return new long[]{ 1L, now };
            }
            entry[0]++;
            return entry;
        });

        long[] bucket = buckets.get(ip);
        if (bucket[0] > MAX_REQUESTS_PER_WINDOW) {
            int count = violations.computeIfAbsent(ip, k -> new AtomicInteger(0))
                                  .incrementAndGet();
            if (count >= BAN_THRESHOLD && "unknown".equals(ip) == false) {
                IpRangeFilter.ban(ip);
            }
            ctx.abortWith(Response.status(429)
                    .entity("{\"error\":\"Too many requests\"}")
                    .header("Retry-After", String.valueOf(WINDOW_MS / 1000))
                    .build());
        }
    }

    /**
     * Resolves the real client IP, honouring a reverse-proxy {@code X-Forwarded-For} header
     * if present.
     */
    private static String remoteIp(ContainerRequestContext ctx) {
        String xff = ctx.getHeaderString("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        Object addr = ctx.getProperty("jakarta.servlet.request.remoteAddr");
        return addr != null ? addr.toString() : "unknown";
    }
}