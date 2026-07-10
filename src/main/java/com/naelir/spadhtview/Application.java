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
            long now = System.currentTimeMillis();
            Object[][] dummyData = dummyData(now);
            for (Object[] row : dummyData) {
                Entry e = new Entry();
                e.name      = (String) row[0];
                e.hash      = (String) row[1];
                e.fileCount = (int)    row[2];
                e.foundTime = (long)   row[3];
                e.size      = (long)   row[4];
                e.genre     = (String) row[5];
                repo.insert(e);
            }
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

    static Object[][] dummyData(long now) {
        Object[][] dummyData = {
            { "The Dark Knight (2008) [1080p BluRay]",   "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2", 2,  now - 86400000L * 1,  2_500_000_000L, "movie"        },
            { "Planet Earth II Complete Series [2160p]", "b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3", 6,  now - 86400000L * 2,  48_000_000_000L,"documentary"  },
            { "Ubuntu 24.04 LTS Desktop amd64",          "c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4", 1,  now - 86400000L * 3,  2_000_000_000L, "software"     },
            { "Inception (2010) [4K HDR HEVC]",          "d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5", 1,  now - 86400000L * 4,  18_000_000_000L,"movie"        },
            { "Complete Works of Shakespeare EPUB",      "e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6", 37, now - 86400000L * 5,  50_000_000L,    "ebook"        },
            { "Interstellar (2014) [1080p BluRay x265]", "f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1", 3,  now - 86400000L * 6,  8_700_000_000L, "movie"        },
            { "Linux From Scratch 12.0 PDF",             "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6b2c3", 1,  now - 86400000L * 7,  5_000_000L,     "ebook"        },
            { "Cosmos A Spacetime Odyssey S01 [720p]",   "b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6c3d4e5", 13, now - 86400000L * 8,  22_000_000_000L,"documentary"  },
            { "VLC Media Player 3.0.20 Windows x64",     "c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6d4e5f6a1", 1,  now - 86400000L * 9,  40_000_000L,    "software"     },
            { "Dune Part Two (2024) [2160p HDR]",        "d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6e5f6a1b2c3", 1,  now - 86400000L * 10, 55_000_000_000L,"movie"        },
        };
        return dummyData;
    }
}