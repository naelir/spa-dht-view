package com.naelir.spadhtview;

import java.io.InputStream;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

/**
 * Serves static files (JS, CSS, images, …) from the classpath {@code /static/} directory.
 *
 * <p>Handles {@code GET /static/{filename}} so that the fat-jar needs no external
 * web server.  Only files that actually exist on the classpath are served; anything
 * else returns 404.
 */
@Path("/")
public class StaticResource {

    private static final Map<String, String> MIME_TYPES = Map.of(
            "js",   "application/javascript;charset=UTF-8",
            "css",  "text/css;charset=UTF-8",
            "html", "text/html;charset=UTF-8",
            "png",  "image/png",
            "ico",  "image/x-icon",
            "svg",  "image/svg+xml",
            "json", "application/json"
    );

    @GET
    @Path("/{filename: .+}")
    public Response serve(@PathParam("filename") String filename) {
        // Prevent path traversal
        if (filename.contains("..")) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        InputStream stream = StaticResource.class.getResourceAsStream("/static/" + filename);
        if (stream == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        String ext  = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1) : "";
        String mime = MIME_TYPES.getOrDefault(ext.toLowerCase(), "application/octet-stream");

        return Response.ok(stream).header("Content-Type", mime).build();
    }
}
