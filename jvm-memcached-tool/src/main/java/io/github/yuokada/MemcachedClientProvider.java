package io.github.yuokada;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.ClientMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.ParseResult;


@ApplicationScoped
public class MemcachedClientProvider {

    private static final Logger logger = LoggerFactory.getLogger(MemcachedClientProvider.class);

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
    MemcachedClient dataSource(ParseResult parseResult) throws IOException {
        logger.debug(String.join(", ", parseResult.expandedArgs()));

        String configEndpoint;
        if (parseResult.hasMatchedOption("host")) {
            configEndpoint = parseResult.matchedOption("host").getValue().toString();
        } else {
            configEndpoint = parseResult.matchedOption("host").defaultValue();
        }
        int clusterPort;
        if (parseResult.hasMatchedOption("p")) {
            clusterPort = Integer.parseInt(parseResult.matchedOption("p").getValue());
        } else {
            clusterPort = Integer.parseInt(parseResult.matchedOption("p").defaultValue());
        }
        logger.info(String.format("server: %s:%d", configEndpoint, clusterPort));

        return getMemcachedClient(configEndpoint, clusterPort);
    }
}