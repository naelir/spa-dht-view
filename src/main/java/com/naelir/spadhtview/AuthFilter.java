package com.naelir.spadhtview;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * HTTP Basic-Auth guard that protects every resource under {@code /*}.
 *
 * <p>Runs {@link PreMatching} so it is invoked before Jersey even resolves
 * which resource will handle the request — no path is reachable without
 * valid credentials.
 *
 * <p>Credentials are hardcoded:
 * <ul>
 *   <li>username: {@code user}</li>
 *   <li>password: {@code resu}</li>
 * </ul>
 */
@Provider
@PreMatching
public class AuthFilter implements ContainerRequestFilter {

    private static final String USERNAME  = "user";
    private static final String PASSWORD  = "resu";
    private static final String REALM     = "dht-view";
    private static final String CHALLENGE = "Basic realm=\"" + REALM + "\"";

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        if (isAuthenticated(ctx)) {
            return;
        }
        ctx.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .header("WWW-Authenticate", CHALLENGE)
                        .entity("Unauthorized")
                        .build());
    }

    private static boolean isAuthenticated(ContainerRequestContext ctx) {
        String header = ctx.getHeaderString("Authorization");
        if (header == null || !header.startsWith("Basic ")) {
            return false;
        }
        try {
            String decoded = new String(
                    Base64.getDecoder().decode(header.substring("Basic ".length()).trim()),
                    StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            if (colon < 1) return false;
            String user = decoded.substring(0, colon);
            String pass = decoded.substring(colon + 1);
            return USERNAME.equals(user) && PASSWORD.equals(pass);
        } catch (IllegalArgumentException e) {
            return false;   // malformed Base64
        }
    }
}
