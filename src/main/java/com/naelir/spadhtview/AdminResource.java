package com.naelir.spadhtview;

import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Lightweight admin-check endpoint.
 *
 * <p>Returns {@code {"admin": true}} when the supplied {@code token} query
 * parameter matches the value of the {@code ADMIN_TOKEN} system property,
 * and {@code {"admin": false}} otherwise.  No credentials are stored on the
 * client – the page simply passes the URL query param on every check.
 *
 * <p>Set the secret at startup:
 * <pre>
 *   java -DADMIN_TOKEN=mysecret -jar spa-dht-view.jar
 * </pre>
 *
 * <p>Then open the page with {@code ?admin=mysecret} to unlock the delete UI.
 */
@Path("/api/admin")
@Produces(MediaType.APPLICATION_JSON)
public class AdminResource {

    private static final String ADMIN_TOKEN =
            System.getProperty("ADMIN_TOKEN", System.getenv("ADMIN_TOKEN") != null
                    ? System.getenv("ADMIN_TOKEN") : "");

    /** Shared helper used by other resources to validate the admin token. */
    public static boolean isValidToken(String token) {
        return !ADMIN_TOKEN.isBlank() && ADMIN_TOKEN.equals(token);
    }

    /**
     * GET /api/admin?token=&lt;token&gt;
     *
     * @return 200 with {@code {"admin":true}} on a match, {@code {"admin":false}} otherwise.
     */
    @GET
    public Response check(@QueryParam("token") String token) {
        boolean granted = isValidToken(token);
        return Response.ok(Map.of("admin", granted)).build();
    }
}
