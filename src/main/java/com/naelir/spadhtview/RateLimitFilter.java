package com.naelir.spadhtview;

import java.util.concurrent.ConcurrentHashMap;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Leaky-bucket rate limiter applied to all {@code /api/} requests.
 *
 * <p>Each unique remote IP is allowed at most {@link #MAX_REQUESTS_PER_WINDOW}
 * requests within a sliding window of {@link #WINDOW_MS} milliseconds.
 * Requests that exceed the limit receive {@code 429 Too Many Requests}.
 *
 * <p>Configurable via system properties:
 * <ul>
 *   <li>{@code rate.limit}        – max requests per window per IP (default 60)</li>
 *   <li>{@code rate.limit.window} – window size in milliseconds (default 60 000 = 1 minute)</li>
 * </ul>
 */
@Provider
public class RateLimitFilter implements ContainerRequestFilter {

    private static final int  MAX_REQUESTS_PER_WINDOW =
            Integer.parseInt(System.getProperty("rate.limit", "10"));
    private static final long WINDOW_MS =
            Long.parseLong(System.getProperty("rate.limit.window", "60000"));

    /** Tracks [requestCount, windowStartMs] per IP. */
    private final ConcurrentHashMap<String, long[]> buckets = new ConcurrentHashMap<>();

    @Override
    public void filter(ContainerRequestContext ctx) {
        String path = ctx.getUriInfo().getPath();
        if (!path.startsWith("api/") && !path.startsWith("/api/")) {
            return; // only guard the REST API
        }

        String ip = remoteIp(ctx);
        long   now = System.currentTimeMillis();

        buckets.compute(ip, (k, entry) -> {
            if (entry == null || now - entry[1] > WINDOW_MS) {
                // new bucket or expired window
                return new long[]{ 1L, now };
            }
            entry[0]++;
            return entry;
        });

        long[] bucket = buckets.get(ip);
        if (bucket[0] > MAX_REQUESTS_PER_WINDOW) {
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
        // ContainerRequestContext does not expose the socket address directly;
        // fall back to a property set by the servlet container.
        Object addr = ctx.getProperty("jakarta.servlet.request.remoteAddr");
        return addr != null ? addr.toString() : "unknown";
    }
}
