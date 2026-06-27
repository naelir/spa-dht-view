package com.naelir.spadhtview;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * Application entry point.
 *
 * <p>Starts an embedded Jetty 12 (EE11) server with the Jersey 4 servlet
 * mounted at {@code /*}.  The {@link IndexResource} handles {@code GET /}
 * (serves the SPA), and {@link EntryResource} handles {@code /api/entries}.
 *
 * <p>Configurable via system properties:
 * <ul>
 *   <li>{@code server.port}  – listening port (default 8080)</li>
 *   <li>{@code repo}         – repository type: {@code memory} or {@code mongo} (default mongo)</li>
 *   <li>{@code mongo.uri}    – MongoDB connection string (default mongodb://localhost:27017)</li>
 *   <li>{@code mongo.db}     – MongoDB database name (default dhtview)</li>
 * </ul>
 */
public class Application {

    public static void main(String[] args) throws Exception {
        int    port     = Integer.parseInt(System.getProperty("SERVER_PORT", "8000"));
        String repoType = System.getProperty("DB_TYPE", "mongo");

        EntryRepository repo;
        if ("memory".equalsIgnoreCase(repoType)) {
            System.out.println("Using InMemoryEntryRepository");
            repo = new InMemoryEntryRepository();
        } else {
            String mongoUri = System.getProperty("DATABASE_URL", "mongodb://localhost:27017");
            String mongoDb  = System.getProperty("DATABASE_NAME", "dht_view");
            String table  = System.getProperty("MAIN_TABLE", "hashes");
            System.out.printf("Using MongoEntryRepository  uri=%s  db=%s%n", mongoUri, mongoDb);
            repo = new MongoEntryRepository(mongoUri, mongoDb, table);
        }

        // ----- Jetty 12 EE11 setup -----
        Server server = new Server(port);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");

        // Jersey 4 servlet – handles everything under /*
        //   GET  /              → IndexResource  (serves the Vue SPA)
        //   GET  /api/entries   → EntryResource  (REST API)
        //   POST /api/entries   → EntryResource
        //   PUT  /api/entries/{id} → EntryResource
        ServletHolder jerseyHolder = new ServletHolder(new ServletContainer(new JerseyConfig(repo)));
        jerseyHolder.setInitOrder(0);
        context.addServlet(jerseyHolder, "/*");

        server.setHandler(context);
        server.start();
        System.out.printf("DHT View started on http://localhost:%d/%n", port);
        server.join();
    }
}