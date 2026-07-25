package com.naelir.spadhtview;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Jersey pre-matching filter that rejects requests from IP addresses that are
 * not allowed according to {@link IpRangeFilter#isAllowed(byte[])}.
 *
 * <p>The requester IP is read from the {@code X-Forwarded-For} header when
 * present (proxy / load-balancer scenario), falling back to the value supplied
 * by the servlet container via the {@code X-Real-IP} header, and finally to
 * the raw remote address stored in the request context property
 * {@code org.apache.catalina.servlet4preview.http.HttpServletRequest} is not
 * available in JAX-RS, so we rely on the standard JAX-RS
 * {@link ContainerRequestContext} only.
 */
@Provider
@PreMatching
public class IpFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String remoteIp = resolveIp(ctx);
        if (remoteIp == null || !isAllowed(remoteIp)) {
            ctx.abortWith(
                    Response.status(Response.Status.FORBIDDEN)
                            .entity("Forbidden")
                            .build());
        }
    }

    /**
     * Resolves the requester IP from JAX-RS headers.
     * Checks {@code X-Forwarded-For} first, then {@code X-Real-IP}.
     */
    private static String resolveIp(ContainerRequestContext ctx) {
        String xff = ctx.getHeaderString("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // X-Forwarded-For may contain a comma-separated list; the first entry is the client.
            return xff.split(",")[0].trim();
        }
        String xri = ctx.getHeaderString("X-Real-IP");
        if (xri != null && !xri.isBlank()) {
            return xri.trim();
        }
        // Fallback: not available via plain JAX-RS ContainerRequestContext.
        return null;
    }

    private static boolean isAllowed(String ip) {
        try {
            byte[] bytes = InetAddress.getByName(ip).getAddress();
            return IpRangeFilter.isAllowed(bytes);
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
