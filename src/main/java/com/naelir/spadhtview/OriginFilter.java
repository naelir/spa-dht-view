package com.naelir.spadhtview;

import org.apache.commons.lang3.StringUtils;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Rejects API requests whose {@code Origin} or {@code Referer} header does not
 * originate from this server.
 *
 * <p>Legitimate browser XHR/fetch requests always carry one of these headers for
 * cross-origin checks; bare spider/curl requests typically send neither.  When
 * neither header is present the request is rejected with {@code 403 Forbidden}.
 *
 * <p>The expected origin host is resolved at startup from the system property
 * {@code server.origin}.  When that property is absent the filter compares
 * against the request's own {@code Host} header, which works correctly when the
 * app is accessed directly (no reverse proxy rewriting the host).
 *
 * <p>Set at startup:
 * <pre>
 *   java -Dserver.origin=https://myapp.example.com -jar spa-dht-view.jar
 * </pre>
 */
@Provider
public class OriginFilter implements ContainerRequestFilter {

    /** Optional override – e.g. {@code https://myapp.example.com}. */
    private static final String EXPECTED_ORIGIN = System.getenv("EXPECTED_ORIGIN");

    @Override
    public void filter(ContainerRequestContext ctx) {
        String path = ctx.getUriInfo().getPath();
        if (!path.startsWith("api/") && !path.startsWith("/api/")) {
            return; // only guard the REST API
        }

        String origin  = ctx.getHeaderString("Origin");
        String referer = ctx.getHeaderString("Referer");
        

        if (origin == null && referer == null) {
            ctx.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .build());
            return;
        }

        String expected = normalizeOrigin(resolveExpected(ctx));
        String candidate = normalizeOrigin(origin != null ? origin : referer);

        if (candidate == null || !candidate.contains(expected)) {
            ctx.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .build());
        }
    }

    /** Returns the configured expected origin, or derives it from the {@code Host} header. */
    private static String resolveExpected(ContainerRequestContext ctx) {
        if (StringUtils.isNotBlank(EXPECTED_ORIGIN)) {
            return EXPECTED_ORIGIN;
        }
        return ctx.getUriInfo().getBaseUri().toString();
    }

    /** Strips the scheme (e.g. {@code https://}) and any trailing {@code /} from the given string. */
    private static String normalizeOrigin(String s) {
        if (s == null)
            return null;
        int idx = s.indexOf("://");
        if (idx >= 0)
            s = s.substring(idx + 3);
        return s;
    }
}
