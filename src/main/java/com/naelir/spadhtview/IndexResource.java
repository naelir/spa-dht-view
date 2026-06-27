package com.naelir.spadhtview;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;

/**
 * Serves the Vue.js SPA entry-point from the classpath.
 * Handles GET / so the fat-jar needs no external web server for static assets.
 */
@Path("/")
public class IndexResource {

    @GET
    @Produces("text/html;charset=UTF-8")
    public Response index() {
        InputStream html = IndexResource.class.getResourceAsStream("/static/index.html");
        if (html == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("index.html not found on classpath")
                    .build();
        }
        return Response.ok(html).build();
    }
}
