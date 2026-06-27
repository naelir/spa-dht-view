package com.naelir.spadhtview;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Jersey 4 application configuration.
 *
 * <ul>
 *   <li>Binds the shared {@link EntryRepository} instance into the HK2 container
 *       so it can be {@code @Inject}ed into JAX-RS resources.</li>
 *   <li>Registers the Jackson JSON provider for automatic (de)serialization.</li>
 * </ul>
 */
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig(EntryRepository repo) {
        // HK2 binder: expose the existing singleton instance
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(repo).to(EntryRepository.class);
            }
        });

        // Basic-Auth guard – runs before any resource is matched
        register(AuthFilter.class);

        // JAX-RS resources
        register(IndexResource.class);
        register(EntryResource.class);

        // Jackson JSON (de)serialization
        register(JacksonFeature.class);
    }
}
