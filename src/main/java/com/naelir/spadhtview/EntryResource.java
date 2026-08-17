package com.naelir.spadhtview;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/entries")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EntryResource {

    @Inject
    private EntryRepository repo;

    /**
     * GET /api/entries?page=1&pageSize=20
     * Returns a paginated list wrapped in a JSON envelope.
     */
    @GET
    public Response list(
            @QueryParam("page")     @DefaultValue("1")  int page,
            @QueryParam("pageSize") @DefaultValue("20") int pageSize,
            @QueryParam("token") String token) {

        if (!TokenValidator.isValidReadToken(token)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        List<Entry> entries = repo.findAll(page, pageSize);
        long total = repo.count();

        Map<String, Object> body = new HashMap<>();
        body.put("entries",    entries);
        body.put("total",      total);
        body.put("page",       page);
        body.put("pageSize",   pageSize);
        body.put("totalPages", (int) Math.ceil((double) total / pageSize));
        return Response.ok(body).build();
    }

    /**
     * GET /api/entries/search?name=foo
     * Returns all entries whose name contains {@code name} (case-insensitive),
     * equivalent to SQL {@code LIKE '%name%'}.
     */
    @GET
    @Path("/search")
    public Response searchByName(@QueryParam("name") String name,
                                 @QueryParam("token") String token) {
        if (!TokenValidator.isValidReadToken(token)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        if (name == null || name.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Query parameter 'name' is required")
                    .build();
        }
        List<Entry> results = repo.findByName(name);
        return Response.ok(results).build();
    }

    /**
     * GET /api/entries/{hash}
     * Returns a single entry by its info-hash, or 404.
     */
    @GET
    @Path("/{hash}")
    public Response getByHash(@PathParam("hash") String hash,
                              @QueryParam("token") String token) {
        if (!TokenValidator.isValidReadToken(token)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        Entry entry = repo.findByHash(hash);
        if (entry == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(entry).build();
    }

    /**
     * DELETE /api/entries/{hash}
     * Removes an existing entry by its info-hash.
     * Returns 204 No Content on success, 404 if not found.
     */
    @DELETE
    @Path("/{hash}")
    public Response delete(@PathParam("hash") String hash,
                           @QueryParam("token") String token) {
        if (!AdminResource.isValidToken(token)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        boolean removed = repo.remove(hash);
        if (!removed) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}