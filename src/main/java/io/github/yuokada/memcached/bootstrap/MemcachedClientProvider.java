package io.github.yuokada.memcached.bootstrap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.ClientMode;
import net.spy.memcached.MemcachedClient;
import org.jboss.logging.Logger;
import picocli.CommandLine.ParseResult;

@ApplicationScoped
public class MemcachedClientProvider {

    private static final Logger logger = Logger.getLogger(MemcachedClientProvider.class);

    private static boolean isConfigEndpoint(String host) {
        return host.contains(".cfg.");
    }

    public static MemcachedClient getMemcachedClient(String endpoint, Integer clusterPort)
        throws IOException {
        String configEndpoint = "localhost";
        logger.debug(String.format("server: %s:%d", configEndpoint, clusterPort));
        InetSocketAddress address = new InetSocketAddress(configEndpoint, clusterPort);
        MemcachedClient client;
        if (isConfigEndpoint(endpoint)) {
            client = new MemcachedClient(
                new BinaryConnectionFactory(ClientMode.Dynamic),
                List.of(address)
            );
        } else {
            client = new MemcachedClient(
                new BinaryConnectionFactory(ClientMode.Static),
                List.of(address)
            );
        }
        return client;
    }

    @Produces
    @ApplicationScoped
    MemcachedClient provideMemcachedClient(ParseResult parseResult) throws IOException {
        logger.debug(String.join(", ", parseResult.expandedArgs()));
        String configEndpoint;
        if (parseResult.hasMatchedOption("host")) {
            configEndpoint = parseResult.matchedOption("host").getValue().toString();
        } else {
            configEndpoint = parseResult.commandSpec().findOption("host").defaultValue();
        }
        int clusterPort;
        if (parseResult.hasMatchedOption("port")) {
            clusterPort = (Integer) parseResult.matchedOption("port").getValue();
        } else {
            clusterPort = Integer.parseInt(
                parseResult.commandSpec().findOption("port").defaultValue()
            );
        }
        return getMemcachedClient(configEndpoint, clusterPort);
    }
}
